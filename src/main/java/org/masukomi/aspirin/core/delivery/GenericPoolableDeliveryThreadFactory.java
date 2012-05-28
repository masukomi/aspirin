package org.masukomi.aspirin.core.delivery;

import java.lang.Thread.State;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.masukomi.aspirin.core.AspirinInternal;

/**
 * <p>This object handles the DeliveryThread thread objects in the ObjectPool.
 * </p>
 *
 * @author Laszlo Solova
 *
 */
public class GenericPoolableDeliveryThreadFactory extends BasePoolableObjectFactory {
	
	/**
	 * This is the ThreadGroup of DeliveryThread objects. On shutdown it is 
	 * easier to close all DeliveryThread threads with usage of this group.
	 */
	private ThreadGroup deliveryThreadGroup = null;
	private ObjectPool myParentPool = null;
	
	/**
	 * This is the counter of created DeliveryThread thread objects.
	 */
	private Integer rdCount = 0;
	private Object rdLock = new Object();
	
	/**
	 * <p>Initialization of this Factory. Prerequisite of right working.</p>
	 * 
	 * @param deliveryThreadGroup The threadgroup which contains the 
	 * DeliveryThread threads.
	 * @param pool The pool which use this factory to create and handle objects.
	 */
	public void init(ThreadGroup deliveryThreadGroup, ObjectPool pool) {
		this.deliveryThreadGroup = deliveryThreadGroup;
		myParentPool = pool;
	}
	
	@Override
	public Object makeObject() throws Exception {
		if( myParentPool == null )
			throw new RuntimeException("Please set the parent pool for right working.");
		DeliveryThread dThread = new DeliveryThread(deliveryThreadGroup);
		synchronized (rdLock) {
			rdCount++;
			dThread.setName(DeliveryThread.class.getSimpleName()+"-"+rdCount);
		}
		dThread.setParentObjectPool(myParentPool);
		AspirinInternal.getConfiguration().getLogger().trace("GenericPoolableDeliveryThreadFactory.makeObject(): New DeliveryThread object created: {}.",dThread.getName());
		return dThread;
	}
	
	@Override
	public void destroyObject(Object obj) throws Exception {
		if( obj instanceof DeliveryThread )
		{
			DeliveryThread dThread = (DeliveryThread)obj;
			AspirinInternal.getConfiguration().getLogger().trace(getClass().getSimpleName()+".destroyObject(): destroy thread {}.",dThread.getName());
			dThread.shutdown();
		}
	}

	@Override
	public boolean validateObject(Object obj) {
		if( obj instanceof DeliveryThread )
		{
			DeliveryThread dThread = (DeliveryThread)obj;
			return
				dThread.isAlive() &&
				(
					dThread.getState().equals(State.NEW) ||
					dThread.getState().equals(State.RUNNABLE) ||
					dThread.getState().equals(State.TIMED_WAITING) ||
					dThread.getState().equals(State.WAITING)
				)
			;
		}
		return false;
	}

}
