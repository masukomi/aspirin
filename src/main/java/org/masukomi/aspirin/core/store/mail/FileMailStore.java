package org.masukomi.aspirin.core.store.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.masukomi.aspirin.core.AspirinInternal;

/**
 * This store implementation is designed to reduce memory 
 * usage of MimeMessage instances. All MimeMessage instance 
 * are stored in files and in weak references too. So 
 * garbage collector can remove all large MimeMessage object 
 * from memory if necessary.
 * 
 * @author Laszlo Solova
 *
 */
public class FileMailStore implements MailStore {
	
	private File rootDir;
	private int subDirCount = 3;
	private Random rand = new Random();
	private Map<String, WeakReference<MimeMessage>> messageMap = new HashMap<String, WeakReference<MimeMessage>>();
	private Map<String, String> messagePathMap = new HashMap<String, String>();
	
	@Override
	public MimeMessage get(String mailid) {
		WeakReference<MimeMessage> msgRef = messageMap.get(mailid);
		MimeMessage msg = null;
		if( msgRef != null )
		{
			msg = msgRef.get();
			if( msg == null )
			{
				try {
					msg = new MimeMessage(Session.getDefaultInstance(System.getProperties()),new FileInputStream(new File(messagePathMap.get(mailid))));
					msgRef = new WeakReference<MimeMessage>(msg);
				} catch (FileNotFoundException e) {
					AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName()+" No file representation found for name "+mailid,e);
				} catch (MessagingException e) {
					AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName()+" There is a messaging exception with name "+mailid,e);
				}
			}
		}
		return msg;
	}
	
	@Override
	public List<String> getMailIds() {
		return new ArrayList<String>(messageMap.keySet());
	}
	
	@Override
	public void init() {
		if( !rootDir.exists() ) { return; }
		File[] subdirs = rootDir.listFiles();
		if( subdirs == null ) { return; }
		for( File subDir : subdirs )
		{
			if( subDir.isDirectory() )
			{
				File[] subdirFiles = subDir.listFiles();
				if( subdirFiles == null ) { continue; }
				for( File msgFile : subdirFiles )
				{
					try {
						MimeMessage msg = new MimeMessage(Session.getDefaultInstance(System.getProperties()),new FileInputStream(msgFile));
						String mailid = AspirinInternal.getMailID(msg);
						synchronized (messageMap) {
							messageMap.put(mailid, new WeakReference<MimeMessage>(msg));
							messagePathMap.put(mailid, msgFile.getAbsolutePath());
						}
					} catch (FileNotFoundException e) {
						AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName()+" No file representation found with name "+msgFile.getAbsolutePath(),e);
					} catch (MessagingException e) {
						AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName()+" There is a messaging exception in file "+msgFile.getAbsolutePath(),e);
					}
				}
			}
		}
	}

	@Override
	public void remove(String mailid) {
		synchronized (messageMap) {
			messageMap.remove(mailid);
			synchronized (messagePathMap) {
				File f = new File(messagePathMap.get(mailid));
				f.delete();
				messagePathMap.remove(mailid);
			}
		}
	}
	
	@Override
	public void set(String mailid, MimeMessage msg) {
		String filepath;
		// Create file path
		if( rootDir == null )
			throw new RuntimeException(getClass().getSimpleName()+" Please set up root directory.");
		String subDirName = String.valueOf(rand.nextInt(subDirCount));
		File dir = new File(rootDir, subDirName);
		if( !dir.exists() )
			dir.mkdirs();
		filepath = new File(dir, mailid+".msg").getAbsolutePath();
		// Save informations
		try {
			File msgFile = new File(filepath);
			if( msgFile.exists() ) { msgFile.delete(); }
			if( !msgFile.exists() ) { msgFile.createNewFile(); }
			msg.writeTo(new FileOutputStream(msgFile));
			synchronized (messageMap) {
				messageMap.put(mailid, new WeakReference<MimeMessage>(msg));
				messagePathMap.put(mailid, filepath);
			}
		} catch (FileNotFoundException e) {
			AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName()+" No file representation found for name "+mailid,e);
		} catch (IOException e) {
			AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName()+" Could not write file for name "+mailid,e);
		} catch (MessagingException e) {
			AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName()+" There is a messaging exception with name "+mailid,e);
		}
	}
	
	public void setRootDir(File rootDir) {
		this.rootDir = rootDir;
	}
	public File getRootDir() {
		return rootDir;
	}
	public void setSubDirCount(int subDirCount) {
		this.subDirCount = subDirCount;
	}
	public int getSubDirCount() {
		return subDirCount;
	}

}
