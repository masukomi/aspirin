package org.masukomi.aspirin.core.listener;

import java.util.ArrayList;
import java.util.List;

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
	}

}
