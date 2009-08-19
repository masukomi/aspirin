package org.masukomi.aspirin.core;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;

/**
 * <p>This object handles the RemoteDelivery thread objects in the ObjectPool.
 * </p>
 *
 * @version $Id$
 *
 */
public class GenericPoolableRemoteDeliveryFactory extends BasePoolableObjectFactory {
	
	/**
	 * This is the ThreadGroup of RemoteDelivery objects. On shutdown it is 
	 * easier to close all RemoteDelivery threads with usage of this group.
	 */
	private ThreadGroup remoteDeliveryThreadGroup = null;
	private ObjectPool myParentPool = null;
	
	/**
	 * This is the counter of created RemoteDelivery thread objects.
	 */
	private Integer rdCount = 0;
	
	/**
	 * <p>Initialization of this Factory. Prerequisite of right working.</p>
	 * 
	 * @param deliveryThreadGroup The threadgroup which contains the 
	 * RemoteDelivery threads.
	 * @param pool The pool which use this factory to create and handle objects.
	 */
	public void init(ThreadGroup deliveryThreadGroup, ObjectPool pool) {
		remoteDeliveryThreadGroup = deliveryThreadGroup;
		myParentPool = pool;
	}
	
	@Override
	public Object makeObject() throws Exception {
		if( myParentPool == null )
			throw new RuntimeException("Please set the parent pool for right working.");
		RemoteDelivery rd = new RemoteDelivery(remoteDeliveryThreadGroup);
		synchronized (rdCount) {
			rdCount++;
			rd.setName(RemoteDelivery.class.getSimpleName()+"-"+rdCount);
		}
		rd.setParentPool(myParentPool);
		Configuration.getInstance().getLog().trace(getClass().getSimpleName()+".makeObject(): New RemoteDelivery object created: "+rd.getName());
		/*
		 * This will be started after first borrow of this object, because the 
		 * first item to process will be set after first borrow too.
		 */
//		rd.start();
		Configuration.getInstance().addListener(rd);
		return rd;
	}

	@Override
	public void destroyObject(Object obj) throws Exception {
		if( obj instanceof RemoteDelivery )
		{
			RemoteDelivery rd = (RemoteDelivery)obj;
			Configuration.getInstance().getLog().trace(getClass().getSimpleName()+".destroyObject(): destroy thread "+rd.getName());
			rd.shutdown();
			Configuration.getInstance().removeListener(rd);
		}
	}

	@Override
	public boolean validateObject(Object obj) {
		if( obj instanceof RemoteDelivery )
		{
			RemoteDelivery rd = (RemoteDelivery)obj;
			return rd.isAlive();
		}
		return false;
	}

}
