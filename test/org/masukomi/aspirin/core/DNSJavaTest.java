/*
 * Created on Jul 10, 2004
 * by Kate Rhodes (masukomi at masukomi dot org)
 * 
 */
package org.masukomi.aspirin.core;

import java.util.Collection;

import junit.framework.TestCase;
import org.xbill.DNS.*;
/**
 * @author masukomi (masukomi at masukomi dot org)
 *
 */
public class DNSJavaTest extends TestCase {

	public static void main(String[] args) {
	}
	
	public void testDNSLookup(){
		// test With Three problematic domains. 
		System.out.println("three problematic domains for MX Record retreival");
		MailQue que = new MailQue();
		
		RemoteDelivery rd = new RemoteDelivery(que, null);
		System.out.println("testing gmx.net");
		Collection mxRecords = rd.getMXRecordsForHost("gmx.net");
		assertFalse(mxRecords == null);
		assertTrue(mxRecords.size() > 0);
		System.out.println("testing green.ch");
		mxRecords = rd.getMXRecordsForHost("green.ch");
		assertFalse(mxRecords == null);
		assertTrue(mxRecords.size() > 0);
		System.out.println("testing tschannen.cc");
		mxRecords = rd.getMXRecordsForHost("tschannen.cc");
		assertFalse(mxRecords == null);
		assertTrue(mxRecords.size() > 0);
	}

}
