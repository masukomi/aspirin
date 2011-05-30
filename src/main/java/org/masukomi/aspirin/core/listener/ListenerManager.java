package org.masukomi.aspirin.core.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.store.queue.DeliveryState;
import org.masukomi.aspirin.core.store.queue.QueueInfo;

/**
 * 
 * @author Laszlo Solova
 *
 */
public class ListenerManager {
	private List<AspirinListener> listenerList = new ArrayList<AspirinListener>();
	
	public void add(AspirinListener listener) {
		synchronized (listenerList) {
			listenerList.add(listener);
		}
	}
	public void remove(AspirinListener listener) {
		synchronized (listenerList) {
			listenerList.remove(listener);
		}
	}
	
	public void notifyListeners(QueueInfo qi) {
		List<AspirinListener> listeners = null;
		synchronized (listenerList) {
			listeners = Collections.unmodifiableList(listenerList);
		}
		if( listeners != null && !listeners.isEmpty() )
		{
			for( AspirinListener listener : listeners )
			{
				if( qi.hasState(DeliveryState.FAILED) )
					listener.delivered(qi.getMailid(), qi.getRecipient(), ResultState.FAILED, qi.getResultInfo());
				else
				if( qi.hasState(DeliveryState.SENT) )
					listener.delivered(qi.getMailid(), qi.getRecipient(), ResultState.SENT, qi.getResultInfo());
				if( AspirinInternal.getDeliveryManager().isCompleted(qi) )
					listener.delivered(qi.getMailid(), qi.getRecipient(), ResultState.FINISHED, qi.getResultInfo());
			}
		}
	}

}
