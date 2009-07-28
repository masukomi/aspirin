/*
 * Created on Jul 10, 2004
 * by Kate Rhodes (masukomi at masukomi dot org)
 * 
 */
package org.masukomi.aspirin.core;

import java.util.Collection;

import javax.mail.URLName;

import junit.framework.TestCase;
/**
 * <p>Test of DNS resolving.</p>
 * 
 * @author masukomi (masukomi at masukomi dot org)
 *
 */
public class DNSJavaTest extends TestCase {

	public static void main(String[] args) {}
	
	/**
	 * DNS test with three problematic domains. 
	 */
	public void testDNSLookup() {
		System.out.println("three problematic domains for MX Record retreival");
		
		RemoteDelivery rd = new RemoteDelivery(new ThreadGroup("tempThreadGroup"));
		System.out.println("testing gmx.net");
		Collection<URLName> mxRecords = rd.getMXRecordsForHost("gmx.net");
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
