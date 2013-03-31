package org.agilewiki.pamailbox;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.agilewiki.pactor.Actor;
import org.agilewiki.pactor.ExceptionHandler;
import org.agilewiki.pactor.ResponseProcessor;
import org.agilewiki.pactor._Request;

/**
 * A default <code>MessageQueue</code> implementation, using a
 * ConcurrentLinkedQueue for cross-mailbox message exchanges, and a ArrayDeque
 * for same-mailbox message exchanges.
 *
 * @author monster
 */
public class DefaultMessageQueue extends ConcurrentLinkedQueue<Message>
        implements MessageQueue {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Local queue for same-mailbox exchanges.
     */
    private final ArrayDeque<Message> localQueue;

    /**
     * Flag helps balance out polling on both internal queues.
     * We don't rally care if true or false, so nor sync and no volatile.
     */
    private boolean locaFirst;

    /**
     * Creates a DefaultMessageQueue, with the given local queue initial size.
     */
    public DefaultMessageQueue(final int initialLocalQueueSize) {
        if (initialLocalQueueSize > INITIAL_LOCAL_QUEUE_SIZE)
            localQueue = new ArrayDeque<Message>(initialLocalQueueSize);
        else
            localQueue = new ArrayDeque<Message>(INITIAL_LOCAL_QUEUE_SIZE);
    }

    /**
     * Creates a new Message instance.
     */
    @Override
    public <E, A extends Actor> Message createMessage(
            final MessageSource source, final A targetActor, final Message old,
            final _Request<E, A> _request, final ExceptionHandler handler,
            final ResponseProcessor<E> rp) {
        return new Message(source, targetActor, old, _request, handler, rp);
    }

    /**
     * Is the queue empty?
     */
    @Override
    public boolean isNonEmpty() {
        return !localQueue.isEmpty() || !isEmpty();
    }

    /**
     * Inserts a new message in the queue.
     *
     * @param local Should be true for same-mailbox exchanges
     * @param msg   The new message
     */
    @Override
    public void offer(final boolean local, final Message msg) {
        if (local) {
            localQueue.offer(msg);
        } else {
            super.offer(msg);
        }
    }

    /**
     * Inserts a new message in the queue.
     *
     * @param local Should be true for same-mailbox exchanges
     * @param msgs   The new messages
     */
    @Override
    public void offer(final Iterable<Message> msgs) {
        for (final Message msg : msgs) {
            super.offer(msg);
        }
    }

    /**
     * Returns one message from the concurrent queue, if any is available.
     */
    @Override
    public Message pollConcurrent() {
        return super.poll();
    }

    /**
     * Returns one message from the local queue, if any is available.
     */
    @Override
    public Message pollLocal() {
        return localQueue.poll();
    }

    /**
     * Returns one message, if any is available.
     */
    @Override
    public Message poll() {
        Message result;
        if (locaFirst) {
            result = localQueue.poll();
            if (result == null) {
                result = super.poll();
            }
        } else {
            result = super.poll();
            if (result == null) {
                result = localQueue.poll();
            }
        }
        // We poll alternatively both queues, so that neither starves.
        locaFirst = !locaFirst;
        return result;
    }
}
