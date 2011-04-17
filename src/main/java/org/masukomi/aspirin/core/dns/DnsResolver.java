package org.masukomi.aspirin.core.dns;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Vector;

import javax.mail.URLName;

import org.masukomi.aspirin.core.Aspirin;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * This object checks all DNS contents and get MX records for emails.
 * TODO Create DNS cache
 * 
 * @author Laszlo Solova
 *
 */
public class DnsResolver {
	
	public static final String SMTP_PROTOCOL_PREFIX = "smtp://";
	
	/**
	 * <p>This method gives back the host name(s) where we can send the email. 
	 * It is copied from it's original place in RemoteDelivery object.</p>
	 * 
	 * <p>First time we ask DNS to find MX record(s) of a domain name. If no MX 
	 * records are found, we check the upper level domains (if exists). At last 
	 * we try to get the domain A record, because the MX server could be same as 
	 * the normal domain handler server. If only upper level domain has MX 
	 * record then we append the A record of original hostname (if exists) as 
	 * first element of record collection. If none of these tries are 
	 * successful, we give back an empty collection.</p>
	 * 
	 * Special Thanks to Tim Motika (tmotika at ionami dot com) for 
	 * his reworking of this method.
	 * 
	 * @param hostName We search the associated MX server of this hostname.
	 * @return Collection of URLName objects. If no MX server found, then it 
	 * gives back an empty collection.
	 * 
	 */
	
	public static Collection<URLName> getMXRecordsForHost(String hostName) {

		Vector<URLName> recordsColl = null;
		try {
			boolean foundOriginalMX = true;
			Record[] records = new Lookup(hostName, Type.MX).run();
			
			/*
			 * Sometimes we should send an email to a subdomain which does not 
			 * have own MX record and MX server. At this point we should find an 
			 * upper level domain and server where we can deliver our email.
			 *  
			 * Example: subA.subB.domain.name has not own MX record and 
			 * subB.domain.name is the mail exchange master of the subA domain 
			 * too.
			 */
			if( records == null || records.length == 0 )
			{
				foundOriginalMX = false;
				String upperLevelHostName = hostName;
				while(		records == null &&
							upperLevelHostName.indexOf(".") != upperLevelHostName.lastIndexOf(".") &&
							upperLevelHostName.lastIndexOf(".") != -1
					)
				{
					upperLevelHostName = upperLevelHostName.substring(upperLevelHostName.indexOf(".")+1);
					records = new Lookup(upperLevelHostName, Type.MX).run();
				}
			}

            if( records != null )
            {
            	// Sort in MX priority (higher number is lower priority)
                Arrays.sort(records, new Comparator<Record>() {
                    @Override
                    public int compare(Record arg0, Record arg1) {
                        return ((MXRecord)arg0).getPriority()-((MXRecord)arg1).getPriority();
                    }
                });
                // Create records collection
                recordsColl = new Vector<URLName>(records.length);
                for (int i = 0; i < records.length; i++)
				{ 
					MXRecord mx = (MXRecord) records[i];
					String targetString = mx.getTarget().toString();
					URLName uName = new URLName(
							SMTP_PROTOCOL_PREFIX +
							targetString.substring(0, targetString.length() - 1)
					);
					recordsColl.add(uName);
				}
            }else
            {
            	foundOriginalMX = false;
            	recordsColl = new Vector<URLName>();
            }
            
            /*
             * If we found no MX record for the original hostname (the upper 
             * level domains does not matter), then we add the original domain 
             * name (identified with an A record) to the record collection, 
             * because the mail exchange server could be the main server too.
			 * 
			 * We append the A record to the first place of the record 
			 * collection, because the standard says if no MX record found then 
			 * we should to try send email to the server identified by the A 
			 * record.
             */
			if( !foundOriginalMX )
			{
				Record[] recordsTypeA = new Lookup(hostName, Type.A).run();
				if (recordsTypeA != null && recordsTypeA.length > 0)
				{
					recordsColl.add(0, new URLName(SMTP_PROTOCOL_PREFIX + hostName));
				}
			}

		} catch (TextParseException e) {
			Aspirin.getConfiguration().getLog().warn("DnsResolver.getMXRecordsForHost(): Failed get MX record for host '"+hostName+"'.",e);
		}

		return recordsColl;
	}
}
