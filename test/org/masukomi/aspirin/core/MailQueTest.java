/*
 * Created on Mar 19, 2004
 *
 * @author kate rhodes (masukomi at masukomi dot org)
 */
package org.masukomi.aspirin.core;

import junit.framework.TestCase;
import javax.mail.internet.*;
import javax.mail.*;

import java.rmi.dgc.VMID;
import java.util.Date;
import java.net.*;
import java.io.*;
import java.util.*;
/**
 * @author masukomi masukomi at masukomi dot org
 * @author Sandeep Mukherjee (msandeep at technologist dot com ) who wrote the mail checking routines in his PopKorn
 * 
 */
public class MailQueTest extends TestCase {
	MimeMessage testMessage;
	String messageId;
	
	
	public static final boolean debug = false;
	public static final int POP3PORT = 110;

	// States of client
	public static final int DISCONNECTED = 0;	
	public static final int CONNECTED = 1;	

	String user1 = "aspirin-test";
	String password1 = "test-aspirin";
	
	String user2 = "aspirin-test2";
	String password2 = "test2-aspirin";
	
	String host = "masukomi.org";
	private int state = DISCONNECTED;

	private Socket socket;
	private BufferedReader is;
	private PrintWriter out;

	
	
	
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(MailQueTest.class);
		System.exit(0);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		Configuration config = Configuration.getInstance();
		config.setMaxAttempts(1);
		config.setPostmaster("root@localhost");
		config.setDeliveryThreads(1);
		config.setRetryInterval(60000);
		
		
		
		
		testMessage = SimpleMimeMessageGenerator.getNewMimeMessage();
		testMessage.addRecipient(Message.RecipientType.TO, new InternetAddress("aspirin-test@masukomi.org"));
		testMessage.addRecipient(Message.RecipientType.CC, new InternetAddress("aspirin-test2@masukomi.org"));
		testMessage.setFrom(new InternetAddress("jUnit-aspirin-test@masukomi.org"));
		
		//TODO get a unique id from the garbage collection package
		messageId = new VMID().toString();
		testMessage.setSubject(messageId);
		
		StringBuffer body = new StringBuffer(System.getProperty("java.version"));
		body.append("\n");
		body.append("This is a test message from MailQueTest sent at ");
		body.append(new Date().toString());
		testMessage.setContent(body.toString(), "text/plain");
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Constructor for MailQueTest.
	 * @param arg0
	 */
	public MailQueTest(String arg0) {
		super(arg0);
		
	}

	public void testQueMail() {

		try {
			SimpleMailWatcherImpl watcher = new SimpleMailWatcherImpl();
			MailQue que = new MailQue();
			
			
			que.addWatcher(watcher);
			System.out.println("Que Size1: " + que.getQueueSize());
			que.queMail(testMessage);
			System.out.println("Que Size2: " + que.getQueueSize());
			long startTime = System.currentTimeMillis();
			//while((System.currentTimeMillis() - startTime) < 180000){
			System.out.println("Waiting for send to complete. This may take a bit. ");
			assertTrue(watcher.blockingSuccessCheck());
			System.out.println("Giving it 60 seconds to be processed by the server. ");
			try {
				Thread.currentThread().sleep(60000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				assertTrue(false);
			}
			System.out.println("please note. If the following fails it may be due to delay in getting the");
			System.out.println("message across the net to the new server, or the server may be being slow. ");
			assertTrue(checkForMessage(messageId, user1, password1));
			assertTrue(checkForMessage(messageId, user2, password2));

			
			System.out.println("Que Size3: " + que.getQueueSize());
			
			if(que.getQueueSize() >0){
				Iterator it = que.getQue().iterator();
				while(it.hasNext()){
					QuedItem qi = (QuedItem)it.next();
					System.out.println("qued item status: " + qi.getStatus() + " for " + qi.getMail().getRecipients());
				}
			}
			
			assertTrue(que.getQueueSize() == 0);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	
	public boolean checkForMessage(String messageId, String user, String password){
		System.out.println("checking for message "+messageId+ " on server");
		try{
		if(user == null || user.equals("")){
			throw new RuntimeException("User Not Specified");
		}
		
		if(password == null || password.equals("")){
			throw new RuntimeException("Password Not Specified");
		}
		if(host == null || host.equals("")){
			throw new RuntimeException("Host Not Specified");
		}
		// Start the connection and send request
		
			//System.out.println("Connecting to " + host + "..");
			socket = new Socket(host, POP3PORT);
			state = CONNECTED;
			//System.out.println("Connected.");
			// We expect only a few lines to go out to server and a
			// flood a data coming in.
			// Makes sense to buffer the input stream but not the output stream.
			is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		

		// Start the handshaking..
		// System.out.println("Sending login info..");
		getResponse(is);
		out.println("USER " + user);
		getResponse(is);
		out.println("PASS " + password);
		getResponse(is);
		System.out.println("Login OK.");

		// Get info about mailbox
		out.println("STAT");
		String droplist = getResponse(is);
		// droplist is of form "+OK nummesg size"
		StringTokenizer st = new StringTokenizer(droplist);
		// Should check for exceptions. 
		int nmesg = Integer.parseInt(st.nextToken());
		int mboxsize = Integer.parseInt(st.nextToken());

		//System.out.println(nmesg + " messages in " + mboxsize + " bytes for " + user);
		boolean found = false;
// Now go thru this list and spew the messages
		for(int i = 1; i <= nmesg; i++)
		{
			//System.out.println("Fetching message " + i + " of " + nmesg);
			out.println("RETR " + i);
			getResponse(is);
			

			String instr = is.readLine();
			while(!instr.equals("."))
			{
				//System.out.println(instr);
				if (instr.equals("Subject: "+messageId)){
					System.out.println("found "+messageId+" on server ");
					found = true;
				}
				instr = is.readLine();
			}

			// End of message. Delete if required
			if(found)
			{
				out.println("DELE " + i);
				getResponse(is);
				return true;
			} /*else {
				System.out.println("Message not found on server. Maybe it hasn't arrived yet?");
			}*/

		}
		

		}catch (Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return false;
	}
	
	
	/**
	 * Parses the response line from server
	 * Throws an exception if not ok.
	 * @param in Stream from where to read the response.
	 * @exception POP3Exception If there was any fatal error.
	 * @return The response message
	 */
	public String getResponse(BufferedReader in) throws IOException
	{
		String str ;
		
			str =  in.readLine();
			if(debug){
				System.out.println(str);
			}
		StringTokenizer st = new StringTokenizer(str);
		String resp = st.nextToken();
		String mesg = st.nextToken("\r\n");	// The whole line
		if(!resp.equalsIgnoreCase("+OK")){
			throw new RuntimeException("POP Error:" + mesg);
		}
		return mesg;

	}

}
