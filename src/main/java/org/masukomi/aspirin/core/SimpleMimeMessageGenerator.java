/*
 * Created on Jun 17, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.masukomi.aspirin.core;

import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * @author masukomi
 *
 * A simple convenience class that generates a MimeMessage for you to fill in
 * without you having to remember / worry about Sessions or Properties or any of that. 
 * No properties are set in the creation of this MimeMessage. 
 * 
 * @deprecated
 */
public class SimpleMimeMessageGenerator {

	static public MimeMessage getNewMimeMessage(){
		return new MimeMessage(Session.getDefaultInstance(new Properties()));
	}

}
