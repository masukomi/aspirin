/*
 * Created on Sep 14, 2004
 * by Kate Rhodes (masukomi at masukomi dot org)
 * 
 */
package org.masukomi.aspirin.core;

import junit.framework.TestCase;

/**
 * @deprecated
 * @author masukomi (masukomi at masukomi dot org)
 *
 */
public class TrackableThreadPoolTest extends TestCase {

	public static void main(String[] args) {
	}

	public final void testIt() {
		//TODO Implement getRunnablesExecutingCount().
		int poolSize =2;
		TrackableThreadPool ttp = new TrackableThreadPool(poolSize);
		for (int i = 0; i < 20; i ++){
			ttp.invokeLater(new TestRunnable(i));
		}
		assertTrue(ttp.getRunnablesExecutingCount() > 0);
		try {
			Thread.currentThread().sleep(1000);
			System.out.println("currently there are " + ttp.getRunnablesExecutingCount() + " threads from the pool of " + poolSize + " that are working");
			assertTrue(ttp.getRunnablesExecutingCount() > 0);
			ttp.stop();
			Thread.currentThread().sleep(300);
			
		} catch (Throwable t){
			System.out.println("oh noooooooo");
			t.printStackTrace();
		}
		assertTrue(ttp.getRunnablesExecutingCount() == 0);

		
	}


	
	public class TestRunnable implements Runnable{
		int number;
		public TestRunnable(int number){
			this.number = number;
		}
		public void run(){
			try {
				Thread.currentThread().sleep(300);
				System.out.println("TestRunnable " + number + " doing my job");
			} catch (Throwable t){
				System.out.println("eeep!");
				t.printStackTrace();
			}
		}
	}

}
