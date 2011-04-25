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
	
	private List<SimpleQueueInfo> queueInfoList = new LinkedList<SimpleQueueInfo>();
	private Map<String, SimpleQueueInfo> queueInfoByMailidAndRecipient = new HashMap<String, SimpleQueueInfo>();
	private Map<String, List<SimpleQueueInfo>> queueInfoByMailid = new HashMap<String, List<SimpleQueueInfo>>();
	private Map<String, List<SimpleQueueInfo>> queueInfoByRecipient = new HashMap<String, List<SimpleQueueInfo>>();
	private Object lock = new Object();
	private Comparator<SimpleQueueInfo> queueInfoComparator = new Comparator<SimpleQueueInfo>() {
		@Override
		public int compare(SimpleQueueInfo o1, SimpleQueueInfo o2) {
			return (int)(o2.getAttempt()-o1.getAttempt());
		}
	};
	
	@Override
	public void add(String mailid, long expiry, Collection<InternetAddress> recipients) throws MessagingException {
		try {
			for( InternetAddress recipient : recipients )
			{
				SimpleQueueInfo queueInfo = new SimpleQueueInfo();
				queueInfo.setExpiry(expiry);
				queueInfo.setMailid(mailid);
				queueInfo.setRecipient(recipient.getAddress());
				synchronized (lock) {
					
					queueInfoList.add(queueInfo);
					
					queueInfoByMailidAndRecipient.put(createSearchKey(queueInfo.getMailid(),queueInfo.getRecipient()), queueInfo);
					
					if( !queueInfoByMailid.containsKey(queueInfo.getMailid()) )
						queueInfoByMailid.put(queueInfo.getMailid(), new ArrayList<SimpleQueueInfo>());
					queueInfoByMailid.get(queueInfo.getMailid()).add(queueInfo);
					
					if( !queueInfoByRecipient.containsKey(queueInfo.getRecipient()) )
						queueInfoByRecipient.put(queueInfo.getRecipient(), new ArrayList<SimpleQueueInfo>());
					queueInfoByRecipient.get(queueInfo.getRecipient()).add(queueInfo);
					
				}
			}
		} catch (Exception e) {
			throw new MessagingException("Message queueing failed: "+mailid, e);
		}
		
	}
	
	@Override
	public QueueInfo createQueueInfo() {
		return new SimpleQueueInfo();
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
		List<SimpleQueueInfo> qibmList = queueInfoByMailid.get(mailid);
		if( qibmList != null )
		{
			for( SimpleQueueInfo sqi : qibmList )
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
			ListIterator<SimpleQueueInfo> queueInfoIt = queueInfoList.listIterator();
			while( queueInfoIt.hasNext() )
			{
				QueueInfo qi = queueInfoIt.next();
				if( qi.hasState(DeliveryState.QUEUED) )
				{
					synchronized (lock) {
						qi.setState(DeliveryState.IN_PROGRESS);
						return qi;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public void remove(String mailid) {
		synchronized (lock) {
			List<SimpleQueueInfo> removeableQueueInfos = queueInfoByMailid.remove(mailid);
			if( removeableQueueInfos != null )
			{
				for( SimpleQueueInfo sqi : removeableQueueInfos )
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
			List<SimpleQueueInfo> removeableQueueInfos = queueInfoByRecipient.remove(recipient);
			if( removeableQueueInfos != null )
			{
				for( SimpleQueueInfo sqi : removeableQueueInfos )
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
			SimpleQueueInfo uniqueQueueInfo = queueInfoByMailidAndRecipient.get(createSearchKey(qi.getMailid(), qi.getRecipient()));
			if( uniqueQueueInfo != null )
			{
				uniqueQueueInfo.setState(qi.getState());
				uniqueQueueInfo.setAttempt(System.currentTimeMillis()+AspirinInternal.getConfiguration().getDeliveryAttemptDelay());
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
