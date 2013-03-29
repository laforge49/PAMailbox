package org.agilewiki.pamailbox.messaging;

import junit.framework.TestCase;
import org.agilewiki.pactor.Mailbox;
import org.agilewiki.pactor.MailboxFactory;
import org.agilewiki.pactor.ResponseProcessor;
import org.agilewiki.pamailbox.DefaultMailboxFactoryImpl;

/**
 * Test code.
 */
public class ThreadBoundTest extends TestCase {
    public void testa() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final Mailbox boundMailbox = mailboxFactory.createThreadBoundMailbox();
        final Mailbox mailbox = mailboxFactory.createMailbox();
        final Actor1 actor1 = new Actor1(mailbox);
        actor1.hi1.send(boundMailbox, new ResponseProcessor<String>() {
            @Override
            public void processResponse(String response) throws Exception {
                assertEquals("Hello world!", response);
            }
        });
        boundMailbox.processMessages();
        mailboxFactory.close();
    }
}
