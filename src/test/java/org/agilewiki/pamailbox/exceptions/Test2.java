package org.agilewiki.pamailbox.exceptions;

import junit.framework.TestCase;

import org.agilewiki.pactor.Mailbox;
import org.agilewiki.pactor.MailboxFactory;
import org.agilewiki.pamailbox.DefaultMailboxFactoryImpl;

/**
 * Test code.
 */
public class Test2 extends TestCase {
    public void testI() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final Mailbox mailbox = mailboxFactory.createMailbox();
        final ActorA actorA = new ActorA(mailbox);
        final ActorB actorB = new ActorB(mailbox);
        try {
            actorB.throwRequest(actorA).pend();
        } catch (final SecurityException se) {
            mailboxFactory.close();
            return;
        }
        throw new Exception("Security exception was not caught");
    }

    /**
     * A SecurityException should be logged.
     */
    public void testII() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final Mailbox mailbox = mailboxFactory.createMailbox();
        final ActorA actorA = new ActorA(mailbox);
        final ActorB actorB = new ActorB(mailbox);
        actorB.throwRequest(actorA).send();
    }

    public void testIII() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final ActorA actorA = new ActorA(mailboxFactory.createMailbox());
        final ActorB actorB = new ActorB(mailboxFactory.createMailbox());
        try {
            actorB.throwRequest(actorA).pend();
        } catch (final SecurityException se) {
            mailboxFactory.close();
            return;
        }
        throw new Exception("Security exception was not caught");
    }

    /**
     * A SecurityException should be logged.
     */
    public void testIV() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactoryImpl();
        final ActorA actorA = new ActorA(mailboxFactory.createMailbox());
        final ActorB actorB = new ActorB(mailboxFactory.createMailbox());
        actorB.throwRequest(actorA).send();
    }
}
