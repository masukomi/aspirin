package org.masukomi.aspirin.core.delivery;

import org.masukomi.aspirin.core.config.Configuration;

/**
 * This interface defines an atomic part of delivery chain.
 * A DeliveryHandler is a particular task in the delivery chain. You can create 
 * your own chain in the {@link Configuration}.
 * 
 * @author Laszlo Solova
 *
 */
public interface DeliveryHandler {
	public void handle(DeliveryContext dCtx) throws DeliveryException;
}
