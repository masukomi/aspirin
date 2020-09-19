/*
 * Created on Jul 10, 2004
 * by Kate Rhodes (masukomi at masukomi dot org)
 *
 */
package org.masukomi.aspirin.core;

import org.junit.Assert;
import org.junit.Test;
import org.masukomi.aspirin.core.dns.DnsResolver;

import javax.mail.URLName;
import java.util.Collection;

/**
 * <p>Test of DNS resolving.</p>
 *
 * @author masukomi (masukomi at masukomi dot org)
 */
public class DNSJavaTest {
    /**
     * DNS test with three problematic domains.
     */
    @Test
    public void testDNSLookup() {
        System.out.println("Three domains with problematic MX records.");
        System.out.println("testing gmx.net");
        Collection<URLName> mxRecords1 = DnsResolver.getMXRecordsForHost("gmx.net");
        Assert.assertNotNull(mxRecords1);
        Assert.assertFalse(mxRecords1.isEmpty());
        System.out.println("testing green.ch");
        Collection<URLName> mxRecords2 = DnsResolver.getMXRecordsForHost("green.ch");
        Assert.assertNotNull(mxRecords2);
        Assert.assertFalse(mxRecords2.isEmpty());
        System.out.println("testing tschannen.cc");
        Collection<URLName> mxRecords3 = DnsResolver.getMXRecordsForHost("tschannen.cc");
        Assert.assertNotNull(mxRecords3);
        Assert.assertFalse(mxRecords3.isEmpty());
    }
}
