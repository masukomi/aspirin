/*
 * Created on May 19, 2004
 * 
 * This class was based on a method in JMTA.java by Rapha‘l Szwarc
 * 
 * 
 * CURRENTLY UNUSED BECAUSE JNDI KEEPS TIMING OUT FOR SOME DOMAINS
 * 
 */
//
//===========================================================================
// 
//Title: JMTA.java
//Description: [Description]
//Author: Rapha‘l Szwarc <zoe (underscore) info (at) mac (dot) com>
//Creation Date: Tue May 18 2004
//Legal: Copyright (C) 2004 Rapha‘l Szwarc
//
//This software is provided 'as-is', without any express or implied warranty.
//In no event will the author be held liable for any damages arising from
//the use of this software.
//
//Permission is granted to anyone to use this software for any purpose,
//including commercial applications, and to alter it and
//redistribute it freely, subject to the following restrictions:
//
//1. The origin of this software must not be misrepresented;
//you must not claim that you wrote the original software.
//If you use this software in a product, an acknowledgment
//in the product documentation would be appreciated but is not required.
//
//2. Altered source versions must be plainly marked as such,
//and must not be misrepresented as being the original software.
//
//3. This notice may not be removed or altered from any source distribution.
//
//---------------------------------------------------------------------------
package org.masukomi.aspirin.core;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import javax.mail.URLName;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/**
 * @author masukomi
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class MXLookup {
	private static final Log log = LogFactory.getLog(MXLookup.class);
	private static final String[] DNSRecords = {"MX"};
	private static final String DNSScheme = "dns://"; //was dns:/
	private static final String SMTPScheme = "smtp://";
	private static Hashtable env;
	
	static public Collection urlsForHost(final String host)
			throws NamingException {
		if (host != null && !host.equals("")) {
			
			if (log.isTraceEnabled()){
				log.trace("looking up mx records for "+host);
			}
			List serviceURLsList = new ArrayList();
			String aQuery = MXLookup.DNSScheme + host;
			if (env == null){
				env = new Hashtable();
				env.put("com.sun.jndi.dns.timeout.initial", "60000");
				//env.put("com.sun.jndi.dns.timeout.retries", "3");
				env.put("com.sun.jndi.dns.recursion", "true");
			}
			
			DirContext dirContext = new InitialDirContext(env);
			
			Attributes dnsAttributes = dirContext.getAttributes(aQuery,
					MXLookup.DNSRecords);
			for (NamingEnumeration anEnumeration = dnsAttributes.getIDs(); anEnumeration
					.hasMore();) {
				String anID = (String) anEnumeration.next();
				Attribute anAttribute = dnsAttributes.get(anID);
				int count = anAttribute.size();
				
				int anIndex;
				for (int index = 0; index < count; index++) {
					Object aValue = anAttribute.get(index);
					if (aValue != null) {
						anIndex = ((String) aValue).indexOf(' ');
						if (anIndex > 0) {
							String aService = ((String) aValue)
									.substring(anIndex + 1);
							URLName aServiceURL = new URLName(
									MXLookup.SMTPScheme + aService);
							serviceURLsList.add(aServiceURL);
						}
					}
				}// END for (int index = 0; index < count; index++)
			} // END for (NamingEnumeration anEnumeration =
			// someAttributes
			//     .getIDs(); anEnumeration.hasMore();)
			dirContext.close();
			if (!serviceURLsList.isEmpty()) {
				//Collections.shuffle(serviceURLsList);
				//return (URLName) serviceURLsList.get(0);
				return serviceURLsList;
			}
			ArrayList defaultArrayList = new ArrayList();
			defaultArrayList.add(new URLName(MXLookup.SMTPScheme + host));
			return defaultArrayList;
		}
		throw new IllegalArgumentException(
				"MXLookup.urlForAddress: null address.");
	}
	
	static public void setJNDIEnvironment(Hashtable environment){
		env = environment;
	}
}