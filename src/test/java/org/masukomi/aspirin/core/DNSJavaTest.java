/*
 * Created on Jul 10, 2004
 * by Kate Rhodes (masukomi at masukomi dot org)
 *
 */
package org.masukomi.aspirin.core;

import java.util.Collection;

import javax.mail.URLName;

import org.junit.Assert;
import org.junit.Test;
import org.masukomi.aspirin.core.dns.DnsResolver;
/**
 * <p>Test of DNS resolving.</p>
 *
 * @author masukomi (masukomi at masukomi dot org)
 *
 */
public class DNSJavaTest {

	/**
	 * DNS test with three problematic domains.
	 */
	@Test
	public void testDNSLookup() {
		System.out.println("Three domains with problematic MX records.");

		System.out.println("testing gmx.net");
		final Collection<URLName> mxRecords1 = DnsResolver.getMXRecordsForHost("gmx.net");
		Assert.assertNotNull(mxRecords1);
		Assert.assertTrue(mxRecords1.size() > 0);

		System.out.println("testing green.ch");
		final Collection<URLName> mxRecords2 = DnsResolver.getMXRecordsForHost("green.ch");
		Assert.assertNotNull(mxRecords2);
		Assert.assertTrue(mxRecords2.size() > 0);

		System.out.println("testing tschannen.cc");
		final Collection<URLName> mxRecords3 = DnsResolver.getMXRecordsForHost("tschannen.cc");
		Assert.assertNotNull(mxRecords3);
		Assert.assertTrue(mxRecords3.size() > 0);
	}

}
