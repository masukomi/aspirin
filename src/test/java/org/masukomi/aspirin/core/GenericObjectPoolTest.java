package org.masukomi.aspirin.core;

import junit.framework.TestCase;

import org.apache.commons.pool.ObjectPool;

/**
 * <p>Test of RemoteDelivery object pool in QueueManager.</p>
 *
 * @version $Id$
 *
 */
public class GenericObjectPoolTest extends TestCase {

	public static void main(String[] args) {
	}

	public void testPoolObject() throws Exception {
		QueManager qm = new QueManager(new MailQue());
		ObjectPool qmPool = qm.getRemoteDeliveryObjectPool();

		RemoteDelivery rd1 = (RemoteDelivery) qmPool.borrowObject();
		assertTrue(rd1 != null && (rd1 instanceof RemoteDelivery));

		qmPool.returnObject(rd1);

		RemoteDelivery rd2 = (RemoteDelivery) qmPool.borrowObject();
		assertTrue(rd2 != null && (rd2 instanceof RemoteDelivery) && rd2.equals(rd1));

		RemoteDelivery rd3 = (RemoteDelivery) qmPool.borrowObject();
		assertTrue(rd3 != null && (rd3 instanceof RemoteDelivery) && !rd2.equals(rd3));

		qmPool.returnObject(rd2);
		qmPool.returnObject(rd3);

		qmPool.clear();
	}

}
