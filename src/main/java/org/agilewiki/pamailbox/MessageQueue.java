package org.agilewiki.pamailbox;

import org.agilewiki.pactor.Actor;
import org.agilewiki.pactor.ExceptionHandler;
import org.agilewiki.pactor.ResponseProcessor;
import org.agilewiki.pactor._Request;

/**
 * A concurrent message queue, used in the Mailbox.
 *
 * @author monster
 */
public interface MessageQueue {
    /**
     * How big should the initial local queue size be?
     */
    int INITIAL_LOCAL_QUEUE_SIZE = 16;

    /**
     * Creates a new Message instance.
     */
    <E, A extends Actor> Message createMessage(
            final MessageSource source,
            final A targetActor,
            final Message old,
            final _Request<E, A> _request,
            final ExceptionHandler handler,
            final ResponseProcessor<E> rp);

    /**
     * Is the queue empty?
     */
    boolean isNonEmpty();

    /**
     * Inserts a new message in the queue.
     *
     * @param e     The new message
     * @param local Should be true for same-mailbox exchanges
     */
    void offer(final Message e, final boolean local);

    /**
     * Retrieves and removes the head of this queue, or returns null if this queue is empty.
     *
     * @return the head of this queue, or null if this queue is empty
     */
    Message poll();

    /**
     * Returns one message from the concurrent queue, if any is available.
     */
    Message pollConcurrent();

    /**
     * Returns one message from the local queue, if any is available.
     */
    Message pollLocal();
}
