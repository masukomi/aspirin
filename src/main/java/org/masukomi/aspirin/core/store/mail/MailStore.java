package org.masukomi.aspirin.core.store.mail;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.mail.internet.MimeMessage;
import java.util.List;

/**
 * This store contain all MimeMessage instances. This is useful,
 * when we try to reduce memory usage, because we can store all
 * MimeMessage objects in files or in RDBMS or in other places,
 * instead of memory.
 *
 * @author Laszlo Solova
 */
public interface MailStore {
    @Nullable
    MimeMessage get(@NotNull String mailid);

    @NotNull
    List<String> getMailIds();

    void init();

    void remove(@Nullable String mailid);

    void set(@NotNull String mailid, @NotNull MimeMessage msg);
}
