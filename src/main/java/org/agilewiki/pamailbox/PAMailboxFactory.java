package org.agilewiki.pamailbox;

import org.agilewiki.pactor.MailboxFactory;

public interface PAMailboxFactory extends MailboxFactory {
    void submit(final Runnable task) throws Exception;
    PAMailbox createMailbox();
    PAMailbox createMailbox(final boolean mayBlock);
    PAMailbox createMailbox(final int initialBufferSize);
    PAMailbox createMailbox(final boolean mayBlock, final int initialBufferSize);
    PAMailbox createMailbox(final boolean mayBlock, final Runnable onIdle);
    PAMailbox createMailbox(final boolean mayBlock, final int initialBufferSize,
                          final Runnable onIdle);
    PAMailbox createThreadBoundMailbox(final Runnable _messageProcessor);
}
