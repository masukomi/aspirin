package org.masukomi.aspirin.core.dns;

import org.jetbrains.annotations.NotNull;
import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.delivery.DeliveryContext;
import org.masukomi.aspirin.core.delivery.DeliveryException;
import org.masukomi.aspirin.core.delivery.DeliveryHandler;

import javax.mail.URLName;
import java.util.Collection;
import java.util.Objects;

/**
 * This delivery handler resolve recipient's MX records and append them to the
 * delivery context.
 * INPUT (REQUIRED) variables:
 * - none
 * OUTPUT (CREATED) variables:
 * - targetservers Collection&lt;URLName&gt;
 *
 * @author Laszlo Solova
 */
public class ResolveHost implements DeliveryHandler {
    @Override
    public void handle(@NotNull DeliveryContext dCtx) throws DeliveryException {
        Objects.requireNonNull(dCtx, "dCtx");
        String currentRecipient = dCtx.getQueueInfo().getRecipient();
        // Get host MX records
        String host = currentRecipient.substring(currentRecipient.lastIndexOf('@') + 1);
        Collection<URLName> targetServers;

        try {
            targetServers = DnsResolver.getMXRecordsForHost(host);
            /*
             * If there was no target server, could be caused by a temporary
             * failure in domain name resolving. So we should to deliver this
             * email later.
             */
            if (targetServers == null || targetServers.isEmpty()) {
                AspirinInternal.getLogger().warn(
                        "ResolveHost.handle(): No mail server found for: '{}'.",
                        host);

                throw new DeliveryException("No MX record found. Temporary failure, trying again.", false);
            }

            AspirinInternal.getLogger().trace(
                    "ResolveHost.handle(): {} servers found for '{}'.",
                    targetServers.size(),
                    host);

            dCtx.addContextVariable("targetservers", targetServers);
        } catch (RuntimeException e) {
            AspirinInternal.getLogger().error(
                    "ResolveHost.handle(): Could not get MX for host '" + host + "' defined by recipient '" + currentRecipient + "'.",
                    e);

            throw new DeliveryException("No MX record found. Temporary failure, trying again.", false, e);
        }
    }
}
