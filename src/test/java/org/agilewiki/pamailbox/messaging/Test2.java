package org.agilewiki.pamailbox.messaging;

import junit.framework.TestCase;

import org.agilewiki.pactor.Mailbox;
import org.agilewiki.pactor.MailboxFactory;
import org.agilewiki.pamailbox.DefaultMailboxFactoryImpl;

/**
 * Test code.
 */
public class Test2 extends TestCase {
    public void testa() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final Mailbox mailbox = mailboxFactory.createMailbox();
        final Actor1 actor1 = new Actor1(mailbox);
        final Actor2 actor2 = new Actor2(mailbox);
        final String result = actor2.hi2(actor1).pend();
        assertEquals("Hello world!", result);
        mailboxFactory.close();
    }

    public void testc() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final Actor1 actor1 = new Actor1(mailboxFactory.createMailbox());
        final Actor2 actor2 = new Actor2(mailboxFactory.createMailbox());
        final String result = actor2.hi2(actor1).pend();
        assertEquals("Hello world!", result);
        mailboxFactory.close();
    }
}
