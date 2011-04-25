package org.masukomi.aspirin.core.listener;

import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.delivery.DeliveryContext;
import org.masukomi.aspirin.core.delivery.DeliveryException;
import org.masukomi.aspirin.core.delivery.DeliveryHandler;

public class NotifyListeners implements DeliveryHandler {

	@Override
	public void handle(DeliveryContext dCtx) throws DeliveryException {
		ListenerManager listenerM = AspirinInternal.getListenerManager();
		if( listenerM == null )
			return;
		listenerM.notifyListeners(dCtx.getQueueInfo());
	}

}
