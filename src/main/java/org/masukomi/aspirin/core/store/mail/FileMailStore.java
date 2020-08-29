package org.masukomi.aspirin.core.store.mail;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.masukomi.aspirin.core.AspirinInternal;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * This store implementation is designed to reduce memory
 * usage of MimeMessage instances. All MimeMessage instance
 * are stored in files and in weak references too. So
 * garbage collector can remove all large MimeMessage object
 * from memory if necessary.
 *
 * @author Laszlo Solova
 */
public class FileMailStore implements MailStore {
    @NotNull
    private final Random rand = new Random();
    @NotNull
    private final Map<String, WeakReference<MimeMessage>> messageMap = new HashMap<>();
    @NotNull
    private final Map<String, String> messagePathMap = new HashMap<>();
    @Nullable
    private File rootDir;
    private int subDirCount = 3;

    @Override
    @Nullable
    public MimeMessage get(@NotNull String mailid) {
        Objects.requireNonNull(mailid, "mailid");
        WeakReference<MimeMessage> msgRef = messageMap.get(mailid);
        MimeMessage msg = null;

        if (msgRef != null) {
            msg = msgRef.get();
            if (msg == null) {
                try {
                    msg = new MimeMessage(Session.getDefaultInstance(System.getProperties()), new FileInputStream(new File(messagePathMap.get(mailid))));
                } catch (FileNotFoundException e) {
                    AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName() + " No file representation found for name " + mailid, e);
                } catch (MessagingException e) {
                    AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName() + " There is a messaging exception with name " + mailid, e);
                }
            }
        }

        return msg;
    }

    @Override
    @NotNull
    public List<String> getMailIds() {
        return new ArrayList<>(messageMap.keySet());
    }

    @Override
    public void init() {
        if (!rootDir.exists()) return;
        File[] subdirs = rootDir.listFiles();
        if (subdirs == null) return;

        Arrays.stream(subdirs)
                .filter(File::isDirectory)
                .map(File::listFiles)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .forEachOrdered(msgFile -> {
                    try {
                        MimeMessage msg = new MimeMessage(Session.getDefaultInstance(System.getProperties()), new FileInputStream(msgFile));
                        String mailid = AspirinInternal.getMailID(msg);

                        synchronized (messageMap) {
                            messageMap.put(mailid, new WeakReference<>(msg));
                            messagePathMap.put(mailid, msgFile.getAbsolutePath());
                        }
                    } catch (FileNotFoundException e) {
                        AspirinInternal.getConfiguration().getLogger().error(
                                getClass().getSimpleName() + " No file representation found with name " + msgFile.getAbsolutePath(),
                                e);
                    } catch (MessagingException e) {
                        AspirinInternal.getConfiguration().getLogger().error(
                                getClass().getSimpleName() + " There is a messaging exception in file " + msgFile.getAbsolutePath(),
                                e);
                    }
                });
    }

    @Override
    public void remove(@Nullable String mailid) {
        synchronized (messageMap) {
            messageMap.remove(mailid);

            synchronized (messagePathMap) {
                File file = new File(messagePathMap.get(mailid));
                file.delete();
                messagePathMap.remove(mailid);
            }
        }
    }

    @Override
    public void set(@NotNull String mailid, @NotNull MimeMessage msg) {
        Objects.requireNonNull(mailid, "mailid");
        Objects.requireNonNull(msg, "msg");
        String filepath;

        // Create file path
        if (rootDir == null)
            throw new IllegalStateException(getClass().getSimpleName() + " Please set up root directory.");

        String subDirName = String.valueOf(rand.nextInt(subDirCount));
        File dir = new File(rootDir, subDirName);

        if (!dir.exists())
            dir.mkdirs();

        filepath = new File(dir, mailid + ".msg").getAbsolutePath();

        // Save information
        try {
            File msgFile = new File(filepath);
            if (msgFile.exists()) {
                msgFile.delete();
            }
            if (!msgFile.exists()) {
                msgFile.createNewFile();
            }
            msg.writeTo(new FileOutputStream(msgFile));
            synchronized (messageMap) {
                messageMap.put(mailid, new WeakReference<>(msg));
                messagePathMap.put(mailid, filepath);
            }
        } catch (FileNotFoundException e) {
            AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName() + " No file representation found for name " + mailid, e);
        } catch (IOException e) {
            AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName() + " Could not write file for name " + mailid, e);
        } catch (MessagingException e) {
            AspirinInternal.getConfiguration().getLogger().error(getClass().getSimpleName() + " There is a messaging exception with name " + mailid, e);
        }
    }

    public @Nullable File getRootDir() {
        return rootDir;
    }

    public void setRootDir(@Nullable File rootDir) {
        this.rootDir = rootDir;
    }

    public int getSubDirCount() {
        return subDirCount;
    }

    public void setSubDirCount(int subDirCount) {
        this.subDirCount = subDirCount;
    }
}
