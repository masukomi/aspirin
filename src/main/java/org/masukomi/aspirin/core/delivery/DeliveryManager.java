package org.masukomi.aspirin.core.delivery;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.config.ConfigurationChangeListener;
import org.masukomi.aspirin.core.config.ConfigurationMBean;
import org.masukomi.aspirin.core.dns.ResolveHost;
import org.masukomi.aspirin.core.store.mail.MailStore;
import org.masukomi.aspirin.core.store.queue.DeliveryState;
import org.masukomi.aspirin.core.store.queue.QueueInfo;
import org.masukomi.aspirin.core.store.queue.QueueStore;

/**
 * This class is the manager of delivery. It is instantiated by Aspirin class.
 * 
 * @author Laszlo Solova
 *
 */
public class DeliveryManager extends Thread implements ConfigurationChangeListener {
	private MailStore mailStore;
	private QueueStore queueStore;
	private Object mailingLock = new Object();
	private ObjectPool deliveryThreadObjectPool = null;
	private boolean running = false;
	private GenericPoolableDeliveryThreadFactory deliveryThreadObjectFactory = null;
	private Map<String, DeliveryHandler> deliveryHandlers = new HashMap<String, DeliveryHandler>();
	
	public DeliveryManager() {
		// Set up default objects.
		this.setName("Aspirin-"+getClass().getSimpleName()+"-"+getId());
		
		// Configure pool of RemoteDelivery threads
		GenericObjectPool.Config gopConf = new GenericObjectPool.Config();
		gopConf.lifo = false;
		gopConf.maxActive = AspirinInternal.getConfiguration().getDeliveryThreadsActiveMax();
		gopConf.maxIdle = AspirinInternal.getConfiguration().getDeliveryThreadsIdleMax();
		gopConf.maxWait = 5000;
		gopConf.testOnReturn = true;
		gopConf.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
		
		// Create RemoteDelivery object factory used in pool
		deliveryThreadObjectFactory = new GenericPoolableDeliveryThreadFactory();
		
		// Create pool
		deliveryThreadObjectPool = new GenericObjectPool(deliveryThreadObjectFactory,gopConf);
		
		// Initialize object factory of pool
		deliveryThreadObjectFactory.init(new ThreadGroup("RemoteDeliveryThreadGroup"),deliveryThreadObjectPool);
		
		// Set up stores and configuration listener 
		queueStore = AspirinInternal.getConfiguration().getQueueStore();
		mailStore = AspirinInternal.getConfiguration().getMailStore();
		
		// Set up deliveryhandlers
		// TODO create by configuration
		deliveryHandlers.put(SendMessage.class.getCanonicalName(), new SendMessage());
		deliveryHandlers.put(ResolveHost.class.getCanonicalName(), new ResolveHost());
		
		AspirinInternal.getConfiguration().addListener(this);
	}
	
	public String add(MimeMessage mimeMessage) throws MessagingException {
		String mailid = AspirinInternal.getMailID(mimeMessage);
		long expiry = AspirinInternal.getExpiry(mimeMessage);
		Collection<InternetAddress> recipients = AspirinInternal.extractRecipients(mimeMessage);
		synchronized (mailingLock) {
			queueStore.add(mailid, expiry, recipients);
			mailStore.set(mailid, mimeMessage);
		}
		return mailid;
	}
	
	public MimeMessage get(QueueInfo qi) {
		return mailStore.get(qi.getMailid());
	}
	
	public void remove(String messageName) {
		synchronized (mailingLock) {
			queueStore.remove(messageName);
			mailStore.remove(messageName);
		}
	}
	
	@Override
	public void run() {
		running = true;
		AspirinInternal.getLogger().info("DeliveryManager started.");
		while( running )
		{
			QueueInfo qi = null;
			try {
				qi = queueStore.next();
				if( qi != null )
				{
					AspirinInternal.getLogger().trace("DeliveryManager.run(): Pool state. A{}/I{}",new Object[]{deliveryThreadObjectPool.getNumActive(),deliveryThreadObjectPool.getNumIdle()});
					DeliveryContext dCtx = new DeliveryContext()
						.setQueueInfo(qi)
						.setMessage(get(qi));
					try 
					{
						AspirinInternal.getLogger().debug("DeliveryManager.run(): Start delivery. qi={}",qi);
						DeliveryThread dThread = (DeliveryThread)deliveryThreadObjectPool.borrowObject();
						AspirinInternal.getLogger().trace("DeliveryManager.run(): Borrow DeliveryThread object. dt={}",dThread.getName());
						dThread.setContext(dCtx);
						/*
						 * On first borrow the DeliveryThread is created and 
						 * initialized, but not started, because the first 
						 * time we have to set up the QueItem to deliver.
						 */
						if( !dThread.isAlive() )
							dThread.start();
					} catch ( IllegalStateException ise )
					{
						/*
						 * This could be happen, if thread is running, but 
						 * ObjectPool is already closed. It is a normal process 
						 * of Aspirin sending thread shutdown.
						 */
						release(qi);
					} catch ( Exception e )
					{
						AspirinInternal.getLogger().error("DeliveryManager.run(): Failed borrow delivery thread object.",e);
						release(qi);
					}
				}
				else
				{
					if( AspirinInternal.getLogger().isTraceEnabled() && 0 < queueStore.size() )
						AspirinInternal.getLogger().trace("DeliveryManager.run(): There is no sendable item in the queue. Fallback to waiting state for a minute.");
					synchronized (this) {
						try
						{
							/*
							 * We should wait for a specified time, because 
							 * some emails unsent could be sendable again.
							 */
							wait(60000);
						}catch (InterruptedException e)
						{
							running = false;
						}
					}
				}
				
			} catch (Throwable t) {
				if( qi != null )
					release(qi);
			}
			
		}
		AspirinInternal.getLogger().info("DeliveryManager terminated.");
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void terminate() {
		running = false;
	}
	
	public void release(QueueInfo qi) {
		if(		qi.isInTimeBounds() &&
				qi.hasState(DeliveryState.IN_PROGRESS, DeliveryState.QUEUED)
			)
		{
			qi.setState(DeliveryState.QUEUED);
		}
		else
		{
			qi.setState(DeliveryState.FAILED);
		}
		queueStore.setSendingResult(qi);
		if( !qi.hasState(DeliveryState.QUEUED) )
			AspirinInternal.getListenerManager().notifyListeners(qi);
		AspirinInternal.getLogger().trace("DeliveryManager.release(): Release item '{}'.",qi.getMailid());
	}
	
	public boolean isCompleted(QueueInfo qi) {
		return queueStore.isCompleted(qi.getMailid());
	}
	
	@Override
	public void configChanged(String parameterName) {
		synchronized (mailingLock) {
			if( parameterName.equals(Configuration.PARAM_MAILSTORE_CLASS) )
				mailStore = AspirinInternal.getConfiguration().getMailStore();
			else
			if( parameterName.equals(Configuration.PARAM_QUEUESTORE_CLASS) )
				queueStore = AspirinInternal.getConfiguration().getQueueStore();
			if( parameterName.equals(ConfigurationMBean.PARAM_DELIVERY_THREADS_ACTIVE_MAX) )
				((GenericObjectPool)deliveryThreadObjectPool).setMaxActive(AspirinInternal.getConfiguration().getDeliveryThreadsActiveMax());
			else
			if( parameterName.equals(ConfigurationMBean.PARAM_DELIVERY_THREADS_IDLE_MAX) )
				((GenericObjectPool)deliveryThreadObjectPool).setMaxIdle(AspirinInternal.getConfiguration().getDeliveryThreadsIdleMax());
		}
	}
	
	public DeliveryHandler getDeliveryHandler(String handlerName) {
		return deliveryHandlers.get(handlerName);
	}
	
	public void shutdown() {
		this.running = false;
		try {
			deliveryThreadObjectPool.close();
			deliveryThreadObjectPool.clear();
		} catch (Exception e) {
			AspirinInternal.getLogger().error("DeliveryManager.shutdown() failed.",e);
		}
	}

}
