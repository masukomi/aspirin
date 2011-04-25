package org.masukomi.aspirin.core.store.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
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
	public void set(String name, MimeMessage msg) {
		String filepath;
		// Create file path
		if( rootDir == null )
			throw new RuntimeException(getClass().getSimpleName()+" Please set up root directory.");
		String subDirName = String.valueOf(rand.nextInt(subDirCount));
		File dir = new File(rootDir, subDirName);
		if( !dir.exists() )
			dir.mkdirs();
		filepath = new File(dir, name+".msg").getAbsolutePath();
		// Save informations
		try {
			messageMap.put(name, new WeakReference<MimeMessage>(msg));
			messagePathMap.put(name, filepath);
			File msgFile = new File(filepath);
			if( msgFile.exists() ) { msgFile.delete(); }
			if( !msgFile.exists() ) { msgFile.createNewFile(); }
			msg.writeTo(new FileOutputStream(msgFile));
		} catch (FileNotFoundException e) {
			AspirinInternal.getConfiguration().getLog().error(getClass().getSimpleName()+" No file representation found for name "+name,e);
		} catch (IOException e) {
			AspirinInternal.getConfiguration().getLog().error(getClass().getSimpleName()+" Could not write file for name "+name,e);
		} catch (MessagingException e) {
			AspirinInternal.getConfiguration().getLog().error(getClass().getSimpleName()+" There is a messaging exception with name "+name,e);
		}
	}

	@Override
	public MimeMessage get(String name) {
		WeakReference<MimeMessage> msgRef = messageMap.get(name);
		MimeMessage msg = null;
		if( msgRef != null )
		{
			msg = msgRef.get();
			if( msg == null )
			{
				try {
					msg = new MimeMessage(Session.getDefaultInstance(System.getProperties()),new FileInputStream(new File(messagePathMap.get(name))));
					msgRef = new WeakReference<MimeMessage>(msg);
				} catch (FileNotFoundException e) {
					AspirinInternal.getConfiguration().getLog().error(getClass().getSimpleName()+" No file representation found for name "+name,e);
				} catch (MessagingException e) {
					AspirinInternal.getConfiguration().getLog().error(getClass().getSimpleName()+" There is a messaging exception with name "+name,e);
				}
			}
		}
		return msg;
	}

	@Override
	public void remove(String name) {
		synchronized (messageMap) {
			messageMap.remove(name);
			synchronized (messagePathMap) {
				File f = new File(messagePathMap.get(name));
				f.delete();
				messagePathMap.remove(name);
			}
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
