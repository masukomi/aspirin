package org.masukomi.aspirin.core.dns;

import org.junit.Assert;
import org.junit.Test;

import javax.mail.URLName;
import java.util.Collection;

public class DNSResolverTests {
    @Test
    public void resolveMXRecords() {
        System.out.println("three problematic domains for MX Record retrieval");

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
