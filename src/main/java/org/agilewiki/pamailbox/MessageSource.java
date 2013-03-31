package org.agilewiki.pamailbox;

import org.agilewiki.pactor.Mailbox;

public interface MessageSource {

    /** Process an incoming response. */
    void incomingResponse(final Message message, final Mailbox responseSource);

    /**
     * Returns true, if the message could be buffered before sending.
     * @param message Message to send-buffer
     * @param target The MessageSource that should eventually receive this message
     * @return true, if buffered
     */
    boolean buffer(final Message message, final MessageSource target);

    /** Adds messages to the queue. */
    void addUnbufferedMessages(final Iterable<Message> messages)
            throws Exception;
}
