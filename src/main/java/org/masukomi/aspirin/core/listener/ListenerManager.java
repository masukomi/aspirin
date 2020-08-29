package org.masukomi.aspirin.core.listener;

import org.jetbrains.annotations.NotNull;
import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.store.queue.DeliveryState;
import org.masukomi.aspirin.core.store.queue.QueueInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Laszlo Solova
 */
public class ListenerManager {
    @NotNull
    private final List<AspirinListener> listenerList = new ArrayList<>();

    public void add(@NotNull AspirinListener listener) {
        Objects.requireNonNull(listener, "listener");

        synchronized (listenerList) {
            listenerList.add(listener);
        }
    }

    public void remove(@NotNull AspirinListener listener) {
        Objects.requireNonNull(listener, "listener");

        synchronized (listenerList) {
            listenerList.remove(listener);
        }
    }

    public void notifyListeners(@NotNull QueueInfo qi) {
        Objects.requireNonNull(qi, "qi");
        List<AspirinListener> listeners;

        synchronized (listenerList) {
            listeners = Collections.unmodifiableList(listenerList);
        }

        if (!listeners.isEmpty()) listeners.forEach(listener -> {
            if (qi.hasState(DeliveryState.FAILED))
                listener.delivered(qi.getMailid(), qi.getRecipient(), ResultState.FAILED, qi.getResultInfo());
            else if (qi.hasState(DeliveryState.SENT))
                listener.delivered(qi.getMailid(), qi.getRecipient(), ResultState.SENT, qi.getResultInfo());
            if (AspirinInternal.getDeliveryManager().isCompleted(qi))
                listener.delivered(qi.getMailid(), qi.getRecipient(), ResultState.FINISHED, qi.getResultInfo());
        });
    }
}
