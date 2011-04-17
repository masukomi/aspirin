package org.masukomi.aspirin.core.listener;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.masukomi.aspirin.core.store.queue.State;

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
	
	public void notifyListeners(MimeMessage message, String recipient, MessagingException mex, State state) {
	}

}
