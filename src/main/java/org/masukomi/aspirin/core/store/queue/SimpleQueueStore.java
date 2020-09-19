package org.masukomi.aspirin.core.store.queue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.masukomi.aspirin.core.AspirinInternal;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.*;

/**
 * @author Laszlo Solova
 */
public class SimpleQueueStore implements QueueStore {
    @NotNull
    private final List<QueueInfo> queueInfoList = new LinkedList<>();
    @NotNull
    private final Map<String, QueueInfo> queueInfoByMailidAndRecipient = new HashMap<>();
    @NotNull
    private final Map<String, Collection<QueueInfo>> queueInfoByMailid = new HashMap<>();
    @NotNull
    private final Map<String, Collection<QueueInfo>> queueInfoByRecipient = new HashMap<>();
    @NotNull
    private final Object lock = new Object();
    @NotNull
    private final Comparator<QueueInfo> queueInfoComparator =
            (o1, o2) -> Long.compare(o2.getAttempt(), o1.getAttempt());

    @NotNull
    private static String createSearchKey(@Nullable String mailid, @Nullable String recipient) {
        return mailid + "-" + recipient;
    }

    @Override
    public void add(@Nullable String mailid, long expiry, @NotNull Iterable<? extends InternetAddress> recipients) throws MessagingException {
        Objects.requireNonNull(recipients, "recipients");

        try {
            recipients.forEach(recipient -> {
                QueueInfo queueInfo = new QueueInfo();
                queueInfo.setExpiry(expiry);
                queueInfo.setMailid(mailid);
                queueInfo.setRecipient(recipient.getAddress());

                synchronized (lock) {
                    queueInfoList.add(queueInfo);
                    queueInfoByMailidAndRecipient.put(createSearchKey(queueInfo.getMailid(), queueInfo.getRecipient()), queueInfo);

                    if (!queueInfoByMailid.containsKey(queueInfo.getMailid()))
                        queueInfoByMailid.put(queueInfo.getMailid(), new ArrayList<>());

                    queueInfoByMailid.get(queueInfo.getMailid()).add(queueInfo);

                    if (!queueInfoByRecipient.containsKey(queueInfo.getRecipient()))
                        queueInfoByRecipient.put(queueInfo.getRecipient(), new ArrayList<>());

                    queueInfoByRecipient.get(queueInfo.getRecipient()).add(queueInfo);

                }
            });
        } catch (RuntimeException e) {
            throw new MessagingException("Message queueing failed: " + mailid, e);
        }
    }

    @NotNull
    @Override
    public List<String> clean() {
        List<String> mailidList;

        synchronized (lock) {
            mailidList = new ArrayList<>(queueInfoByMailid.keySet());
        }

        Iterator<String> mailidIt = mailidList.iterator();

        while (mailidIt.hasNext()) {
            String mailid = mailidIt.next();

            if (isCompleted(mailid)) {
                remove(mailid);
                mailidIt.remove();
            }
        }

        return mailidList;
    }

    @NotNull
    @Override
    public QueueInfo createQueueInfo() {
        return new QueueInfo();
    }

    @Override
    public long getNextAttempt(@Nullable String mailid, @Nullable String recipient) {
        QueueInfo qInfo = queueInfoByMailidAndRecipient.get(createSearchKey(mailid, recipient));
        if (qInfo != null && qInfo.hasState(DeliveryState.QUEUED))
            return qInfo.getAttempt();
        return -1L;
    }

    @Override
    public boolean hasBeenRecipientHandled(@Nullable String mailid, @Nullable String recipient) {
        QueueInfo qInfo = queueInfoByMailidAndRecipient.get(createSearchKey(mailid, recipient));
        return (qInfo != null && qInfo.hasState(DeliveryState.FAILED, DeliveryState.SENT));
    }

    @Override
    public void init() {
        // Do nothing
    }

    @Override
    public boolean isCompleted(@Nullable String mailid) {
        Collection<QueueInfo> qibmList = queueInfoByMailid.get(mailid);

        if (qibmList != null)
            return qibmList.stream().noneMatch(sqi -> sqi.hasState(DeliveryState.IN_PROGRESS, DeliveryState.QUEUED));

        return true;
    }

    @Override
    @Nullable
    public QueueInfo next() {
        queueInfoList.sort(queueInfoComparator);

        if (!queueInfoList.isEmpty())
            synchronized (lock) {
                for (QueueInfo qi : queueInfoList)
                    if (qi.isSendable()) {
                        if (!qi.isInTimeBounds()) {
                            if (qi.getResultInfo() == null || qi.getResultInfo().isEmpty())
                                qi.setResultInfo("Delivery is out of time or attempt.");

                            qi.setState(DeliveryState.FAILED);
                            setSendingResult(qi);
                        } else {
                            qi.setState(DeliveryState.IN_PROGRESS);
                            return qi;
                        }
                    }
            }

        return null;
    }

    @Override
    public void remove(@Nullable String mailid) {
        synchronized (lock) {
            Iterable<QueueInfo> removeableQueueInfos = queueInfoByMailid.remove(mailid);

            if (removeableQueueInfos != null) removeableQueueInfos.forEach(sqi -> {
                queueInfoByMailidAndRecipient.remove(createSearchKey(sqi.getMailid(), sqi.getRecipient()));
                queueInfoByRecipient.get(sqi.getRecipient()).remove(sqi);
            });
        }
    }

    @Override
    public void removeRecipient(@Nullable String recipient) {
        synchronized (lock) {
            Iterable<QueueInfo> removeableQueueInfos = queueInfoByRecipient.remove(recipient);

            if (removeableQueueInfos != null) removeableQueueInfos.forEach(sqi -> {
                queueInfoByMailidAndRecipient.remove(createSearchKey(sqi.getMailid(), sqi.getRecipient()));
                queueInfoByMailid.get(sqi.getMailid()).remove(sqi);
            });
        }
    }

    @Override
    public void setSendingResult(@NotNull QueueInfo qi) {
        Objects.requireNonNull(qi, "qi");

        synchronized (lock) {
            QueueInfo uniqueQueueInfo = queueInfoByMailidAndRecipient.get(createSearchKey(qi.getMailid(), qi.getRecipient()));

            if (uniqueQueueInfo != null) {
                uniqueQueueInfo.setAttempt(System.currentTimeMillis() + AspirinInternal.getConfiguration().getDeliveryAttemptDelay());
                uniqueQueueInfo.incAttemptCount();
                uniqueQueueInfo.setState(qi.getState());
            }
        }
    }

    @Override
    public int size() {
        return queueInfoByMailid.size();
    }
}
