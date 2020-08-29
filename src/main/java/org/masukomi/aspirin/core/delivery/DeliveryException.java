package org.masukomi.aspirin.core.delivery;

import org.jetbrains.annotations.Nullable;

import javax.mail.MessagingException;

/**
 * @author Laszlo Solova
 */
public class DeliveryException extends MessagingException {
    private static final long serialVersionUID = -5388667812025531029L;

    private final boolean permanent;

    public DeliveryException() {
        permanent = true;
    }

    public DeliveryException(@Nullable String s, boolean permanent) {
        super(s);
        this.permanent = permanent;
    }

    public DeliveryException(@Nullable String s, boolean permanent, Exception e) {
        super(s, e);
        this.permanent = permanent;
    }

    public boolean isPermanent() {
        return permanent;
    }
}
