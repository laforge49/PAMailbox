package org.agilewiki.pamailbox;

import org.agilewiki.pactor.MailboxFactory;

public interface _MailboxFactory extends MailboxFactory {
    void submit(final Runnable task) throws Exception;
}
