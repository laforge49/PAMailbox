package org.agilewiki.pamailbox;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.agilewiki.pactor.Mailbox;
import org.agilewiki.pactor.MailboxFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The MailboxFactory is the factory as the name suggests for the MailBoxes to be used with the PActor. In addition to
 * creation of the Mailboxes it also encapsulates the threads( threadpool) which would process the Requests added to
 * the mailbox in asynchronous mode.
 * </p>
 */

public class DefaultMailboxFactoryImpl implements _MailboxFactory {
    /**
     * The SINGLETON instance will be lazily created by the first access to it.
     * No access means no creation. Note that in a JVM with class-GC, if it
     * is not used anymore, it might eventually get GCed.
     */
    private static final class LazyHolder {
        //todo eliminate the static--OSGi incompatible
        public static final MailboxFactory SINGLETON = new DefaultMailboxFactoryImpl();
    }

    private final Logger mailboxLog = LoggerFactory.getLogger(Mailbox.class);

    private final Logger log = LoggerFactory.getLogger(MailboxFactory.class);

    private final ExecutorService executorService;
    private final boolean ownsExecutorService;
    private final MessageQueueFactory messageQueueFactory;
    /** Must also be thread-safe. */
    private final List<AutoCloseable> closables = new Vector<AutoCloseable>();
    private final AtomicBoolean shuttingDown = new AtomicBoolean();
    /** How big should the initial local queue size be? */
    private final int initialLocalMessageQueueSize;
    /** How big should the initial (per target Mailbox) buffer size be? */
    private final int initialBufferSize;

    public DefaultMailboxFactoryImpl() {
        this(null, true, null, MessageQueue.INITIAL_LOCAL_QUEUE_SIZE,
                MessageQueue.INITIAL_BUFFER_SIZE);
    }

    public DefaultMailboxFactoryImpl(final ExecutorService executorService,
            final boolean ownsExecutorService) {
        this(executorService, ownsExecutorService, null,
                MessageQueue.INITIAL_LOCAL_QUEUE_SIZE,
                MessageQueue.INITIAL_BUFFER_SIZE);
    }

    public DefaultMailboxFactoryImpl(final ExecutorService executorService,
            final boolean ownsExecutorService,
            final MessageQueueFactory messageQueueFactory,
            final int initialLocalMessageQueueSize, final int initialBufferSize) {
        this.executorService = (executorService == null) ? Executors
                .newCachedThreadPool() : executorService;
        this.messageQueueFactory = (messageQueueFactory == null) ? DefaultMessageQueueFactoryImpl.INSTANCE
                : messageQueueFactory;
        this.initialLocalMessageQueueSize = initialLocalMessageQueueSize;
        this.ownsExecutorService = ownsExecutorService;
        this.initialBufferSize = initialBufferSize;
    }

    @Override
    public final Mailbox createMailbox() {
        return createMailbox(false, initialBufferSize, null);
    }

    @Override
    public final Mailbox createMailbox(final boolean _mayBlock) {
        return createMailbox(_mayBlock, initialBufferSize, null);
    }

    @Override
    public final Mailbox createMailbox(final boolean _mayBlock,
            final Runnable _onIdle) {
        return createMailbox(_mayBlock, _onIdle, null, this,
                messageQueueFactory
                        .createMessageQueue(initialLocalMessageQueueSize),
                mailboxLog, initialBufferSize);
    }

    @Override
    public final Mailbox createMailbox(final int initialBufferSize) {
        return createMailbox(false, initialBufferSize, null);
    }

    @Override
    public final Mailbox createMailbox(final boolean _mayBlock,
            final int initialBufferSize) {
        return createMailbox(_mayBlock, initialBufferSize, null);
    }

    @Override
    public final Mailbox createMailbox(final boolean _mayBlock,
            final int initialBufferSize, final Runnable _onIdle) {
        return createMailbox(_mayBlock, _onIdle, null, this,
                messageQueueFactory
                        .createMessageQueue(initialLocalMessageQueueSize),
                mailboxLog, initialBufferSize);
    }

    @Override
    public final Mailbox createThreadBoundMailbox(
            final Runnable _messageProcessor) {
        return createMailbox(true, null, _messageProcessor, this,
                messageQueueFactory
                        .createMessageQueue(initialLocalMessageQueueSize),
                mailboxLog, initialBufferSize);
    }

    public final Mailbox createMailbox(final boolean _disableCommandeering,
            final Runnable _onIdle, final MessageQueue messageQueue) {
        return createMailbox(_disableCommandeering, _onIdle, null, this,
                messageQueue, mailboxLog, initialBufferSize);
    }

    @Override
    public final void submit(final Runnable task) throws Exception {
        try {
            executorService.submit(task);
        } catch (final Exception e) {
            if (!isClosing())
                throw e;
            else
                log.warn(
                        "Unable to process the request, as mailbox shutdown had been called in the application",
                        e);
        } catch (final Error e) {
            if (!isClosing())
                throw e;
        }
    }

    @Override
    public final boolean addAutoClosable(final AutoCloseable closeable) {
        if (!isClosing()) {
            return closables.add(closeable);
        } else {
            throw new IllegalStateException("Shuting down ...");
        }
    }

    @Override
    public final boolean removeAutoClosable(final AutoCloseable closeable) {
        if (!isClosing()) {
            return closables.remove(closeable);
        } else {
            throw new IllegalStateException("Shuting down ...");
        }
    }

    @Override
    public final void close() throws Exception {
        if (shuttingDown.compareAndSet(false, true)) {
            if (ownsExecutorService) {
                executorService.shutdownNow();
            }
            final Iterator<AutoCloseable> it = closables.iterator();
            while (it.hasNext()) {
                try {
                    it.next().close();
                } catch (final Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    @Override
    public final boolean isClosing() {
        return shuttingDown.get();
    }

    /** Lazily creates a default MailboxFactory instance, and returns it. */
    public static MailboxFactory singleton() {
        return LazyHolder.SINGLETON;
    }

    /**
     * Actually instantiate the Mailbox.
     * Can be overridden, to create application-specific Mailbox instances.
     */
    protected Mailbox createMailbox(final boolean _mayBlock,
            final Runnable _onIdle, final Runnable _messageProcessor,
            final _MailboxFactory factory, final MessageQueue messageQueue,
            final Logger _log, final int _initialBufferSize) {
        return new MailboxImpl(_mayBlock, _onIdle, _messageProcessor, factory,
                messageQueue, _log, _initialBufferSize);
    }
}
