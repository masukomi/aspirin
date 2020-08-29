package org.masukomi.aspirin.core.delivery;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.masukomi.aspirin.core.AspirinInternal;

/**
 * <p>This object handles the DeliveryThread thread objects in the ObjectPool.
 * </p>
 *
 * @author Laszlo Solova
 */
public class GenericPoolableDeliveryThreadFactory extends BasePoolableObjectFactory {
    @NotNull
    private final Object rdLock = new Object();
    /**
     * This is the ThreadGroup of DeliveryThread objects. On shutdown it is
     * easier to close all DeliveryThread threads with usage of this group.
     */
    @Nullable
    private ThreadGroup deliveryThreadGroup;
    @Nullable
    private ObjectPool myParentPool;
    /**
     * This is the counter of created DeliveryThread thread objects.
     */
    private int rdCount;

    /**
     * <p>Initialization of this Factory. Prerequisite of right working.</p>
     *
     * @param deliveryThreadGroup The threadgroup which contains the
     *                            DeliveryThread threads.
     * @param pool                The pool which use this factory to create and handle objects.
     */
    public void init(@Nullable ThreadGroup deliveryThreadGroup, @Nullable ObjectPool pool) {
        this.deliveryThreadGroup = deliveryThreadGroup;
        myParentPool = pool;
    }

    @Override
    @NotNull
    public Object makeObject() {
        if (myParentPool == null)
            throw new IllegalStateException("Please set the parent pool for right working.");

        DeliveryThread dThread = new DeliveryThread(deliveryThreadGroup);

        synchronized (rdLock) {
            rdCount++;
            dThread.setName(DeliveryThread.class.getSimpleName() + "-" + rdCount);
        }

        dThread.setParentObjectPool(myParentPool);

        AspirinInternal.getConfiguration()
                .getLogger()
                .trace("GenericPoolableDeliveryThreadFactory.makeObject(): New DeliveryThread object created: {}.",
                        dThread.getName());

        return dThread;
    }

    @Override
    public void destroyObject(@Nullable Object obj) {
        if (obj instanceof DeliveryThread) {
            DeliveryThread dThread = (DeliveryThread) obj;

            AspirinInternal.getConfiguration()
                    .getLogger()
                    .trace(getClass().getSimpleName() + ".destroyObject(): destroy thread {}.", dThread.getName());

            dThread.shutdown();
        }
    }

    @Override
    public boolean validateObject(@Nullable Object obj) {
        if (obj instanceof DeliveryThread) {
            DeliveryThread dThread = (DeliveryThread) obj;

            return dThread.isAlive() && (dThread.getState() == Thread.State.NEW ||
                    dThread.getState() == Thread.State.RUNNABLE || dThread.getState() == Thread.State.TIMED_WAITING ||
                    dThread.getState() == Thread.State.WAITING);
        }

        return false;
    }
}
