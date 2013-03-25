package org.agilewiki.pamailbox.exceptions;

import junit.framework.TestCase;

import org.agilewiki.pactor.Mailbox;
import org.agilewiki.pactor.MailboxFactory;
import org.agilewiki.pamailbox.DefaultMailboxFactoryImpl;

/**
 * Test code.
 */
public class Test3 extends TestCase {
    public void testI() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final Mailbox mailbox = mailboxFactory.createMailbox();
        final ActorC actorC = new ActorC(mailbox);
        final String result = actorC.throwRequest.pend();
        assertEquals("java.lang.SecurityException: thrown on request", result);
    }

    public void testII() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final Mailbox mailbox = mailboxFactory.createMailbox();
        final ActorC actorC = new ActorC(mailbox);
        actorC.throwRequest.send();
    }
}
