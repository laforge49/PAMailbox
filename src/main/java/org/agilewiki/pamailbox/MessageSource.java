package org.agilewiki.pamailbox;

import org.agilewiki.pactor.Mailbox;

public interface MessageSource {
    void incomingResponse(final Message message, final Mailbox responseSource);
}
