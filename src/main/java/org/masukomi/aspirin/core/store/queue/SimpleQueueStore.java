package org.masukomi.aspirin.core.store.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.masukomi.aspirin.core.AspirinInternal;


/**
 * 
 * @author Laszlo Solova
 *
 */
public class SimpleQueueStore implements QueueStore {
	
	private List<QueueInfo> queueInfoList = new LinkedList<QueueInfo>();
	private Map<String, QueueInfo> queueInfoByMailidAndRecipient = new HashMap<String, QueueInfo>();
	private Map<String, List<QueueInfo>> queueInfoByMailid = new HashMap<String, List<QueueInfo>>();
	private Map<String, List<QueueInfo>> queueInfoByRecipient = new HashMap<String, List<QueueInfo>>();
	private Object lock = new Object();
	private Comparator<QueueInfo> queueInfoComparator = new Comparator<QueueInfo>() {
		@Override
		public int compare(QueueInfo o1, QueueInfo o2) {
			return (int)(o2.getAttempt()-o1.getAttempt());
		}
	};
	
	@Override
	public void add(String mailid, long expiry, Collection<InternetAddress> recipients) throws MessagingException {
		try {
			for( InternetAddress recipient : recipients )
			{
				QueueInfo queueInfo = new QueueInfo();
				queueInfo.setExpiry(expiry);
				queueInfo.setMailid(mailid);
				queueInfo.setRecipient(recipient.getAddress());
				synchronized (lock) {
					
					queueInfoList.add(queueInfo);
					
					queueInfoByMailidAndRecipient.put(createSearchKey(queueInfo.getMailid(),queueInfo.getRecipient()), queueInfo);
					
					if( !queueInfoByMailid.containsKey(queueInfo.getMailid()) )
						queueInfoByMailid.put(queueInfo.getMailid(), new ArrayList<QueueInfo>());
					queueInfoByMailid.get(queueInfo.getMailid()).add(queueInfo);
					
					if( !queueInfoByRecipient.containsKey(queueInfo.getRecipient()) )
						queueInfoByRecipient.put(queueInfo.getRecipient(), new ArrayList<QueueInfo>());
					queueInfoByRecipient.get(queueInfo.getRecipient()).add(queueInfo);
					
				}
			}
		} catch (Exception e) {
			throw new MessagingException("Message queueing failed: "+mailid, e);
		}
		
	}
	
	@Override
	public QueueInfo createQueueInfo() {
		return new QueueInfo();
	}
	
	@Override
	public long getNextAttempt(String mailid, String recipient) {
		QueueInfo qInfo = queueInfoByMailidAndRecipient.get(createSearchKey(mailid, recipient));
		if( qInfo != null && qInfo.hasState(DeliveryState.QUEUED) )
			return qInfo.getAttempt();
		return -1;
	}

	@Override
	public boolean hasBeenRecipientHandled(String mailid, String recipient) {
		QueueInfo qInfo = queueInfoByMailidAndRecipient.get(createSearchKey(mailid, recipient));
		return ( qInfo != null && qInfo.hasState(DeliveryState.FAILED, DeliveryState.SENT) );
	}

	@Override
	public boolean isCompleted(String mailid) {
		List<QueueInfo> qibmList = queueInfoByMailid.get(mailid);
		if( qibmList != null )
		{
			for( QueueInfo sqi : qibmList )
			{
				if( sqi.hasState(DeliveryState.IN_PROGRESS, DeliveryState.QUEUED) )
					return false;
			}
		}
		return true;
	}
	
	@Override
	public QueueInfo next() {
		Collections.sort(queueInfoList, queueInfoComparator);
		if( !queueInfoList.isEmpty() )
		{
			ListIterator<QueueInfo> queueInfoIt = queueInfoList.listIterator();
			while( queueInfoIt.hasNext() )
			{
				QueueInfo qi = queueInfoIt.next();
				if( qi.isSendable() ) {
					synchronized (lock) {
						if( !qi.isInTimeBounds() )
						{
							qi.setResultInfo("Delivery is out of time or attempt.");
							qi.setState(DeliveryState.FAILED);
							setSendingResult(qi);
						}
						else
						{	
							qi.setState(DeliveryState.IN_PROGRESS);
							return qi;
						}
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public void remove(String mailid) {
		synchronized (lock) {
			List<QueueInfo> removeableQueueInfos = queueInfoByMailid.remove(mailid);
			if( removeableQueueInfos != null )
			{
				for( QueueInfo sqi : removeableQueueInfos )
				{
					queueInfoByMailidAndRecipient.remove(createSearchKey(sqi.getMailid(), sqi.getRecipient()));
					queueInfoByRecipient.get(sqi.getRecipient()).remove(sqi);
				}
			}
		}
	}

	@Override
	public void removeRecipient(String recipient) {
		synchronized (lock) {
			List<QueueInfo> removeableQueueInfos = queueInfoByRecipient.remove(recipient);
			if( removeableQueueInfos != null )
			{
				for( QueueInfo sqi : removeableQueueInfos )
				{
					queueInfoByMailidAndRecipient.remove(createSearchKey(sqi.getMailid(), sqi.getRecipient()));
					queueInfoByMailid.get(sqi.getMailid()).remove(sqi);
				}
			}
		}
	}

	@Override
	public void setSendingResult(QueueInfo qi) {
		synchronized (lock) {
			QueueInfo uniqueQueueInfo = queueInfoByMailidAndRecipient.get(createSearchKey(qi.getMailid(), qi.getRecipient()));
			if( uniqueQueueInfo != null )
			{
				uniqueQueueInfo.setAttempt(System.currentTimeMillis()+AspirinInternal.getConfiguration().getDeliveryAttemptDelay());
				uniqueQueueInfo.incAttemptCount();
				uniqueQueueInfo.setState(qi.getState());
			}
		}
	}
	
	@Override
	public int size() {
		return queueInfoByMailid.size();
	}
	
	private String createSearchKey(String mailid, String recipient) {
		return mailid+"-"+recipient;
	}

}
