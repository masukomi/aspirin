package org.masukomi.aspirin.core.dns;

import java.util.Collection;

import javax.mail.URLName;

import junit.framework.Assert;

import org.junit.Test;

public class DNSResolverTests {
	
	@Test
	public void resolveMXRecords() {
		System.out.println("three problematic domains for MX Record retreival");

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
