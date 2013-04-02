package org.agilewiki.pamailbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.agilewiki.pactor.Actor;
import org.agilewiki.pactor.ExceptionHandler;
import org.agilewiki.pactor.Mailbox;
import org.agilewiki.pactor.MailboxFactory;
import org.agilewiki.pactor.ResponseProcessor;
import org.agilewiki.pactor._Request;
import org.slf4j.Logger;

public class MailboxImpl implements Mailbox, Runnable, MessageSource {

    private final Logger log;

    private final _MailboxFactory mailboxFactory;
    private final MessageQueue inbox;
    private final AtomicBoolean running = new AtomicBoolean();
    private final boolean commandeeringDisabled; //todo: disable commandeering when true
    private final Runnable onIdle;
    private final Runnable messageProcessor;
    private final int initialBufferSize;

    /**
     * Send buffer
     */
    private Map<MessageSource, List<Message>> sendBuffer;

    private ExceptionHandler exceptionHandler;
    private Message currentMessage;

    /**
     * messageQueue can be null to use the default queue implementation.
     */
    public MailboxImpl(final boolean _mayBlock, final Runnable _onIdle,
            final Runnable _messageProcessor, final _MailboxFactory factory,
            final MessageQueue messageQueue, final Logger _log,
            final int _initialBufferSize) {
        commandeeringDisabled = _mayBlock;
        onIdle = _onIdle;
        messageProcessor = _messageProcessor;
        running.set(messageProcessor != null);
        mailboxFactory = factory;
        inbox = messageQueue;
        log = _log;
        initialBufferSize = _initialBufferSize;
    }

    @Override
    public final boolean isEmpty() {
        return !inbox.isNonEmpty();
    }

    /**
     * Flushes buffered messages, if any.
     * Returns true if there was any.
     *
     * @throws Exception
     */
    @Override
    public final boolean flush() throws Exception {
        boolean result = false;
        if (sendBuffer != null) {
            final Iterator<Entry<MessageSource, List<Message>>> iter = sendBuffer
                    .entrySet().iterator();
            while (iter.hasNext()) {
                result = true;
                final Entry<MessageSource, List<Message>> entry = iter.next();
                final MessageSource target = entry.getKey();
                final List<Message> messages = entry.getValue();
                iter.remove();
                target.addUnbufferedMessages(messages);
            }
        }
        return result;
    }

    @Override
    public final <A extends Actor> void signal(final _Request<Void, A> request,
            final A targetActor) throws Exception {
        final Message message = inbox.createMessage(null, targetActor, null,
                request, null, EventResponseProcessor.SINGLETON);
        // No source mean never local and no buffering.
        addMessage(null, message, false);
    }

    /**
     * Same as signal(Request) until buffered message are implemented.
     */
    @Override
    public final <A extends Actor> void signal(final _Request<Void, A> request,
            final Mailbox source, final A targetActor) throws Exception {
        final MailboxImpl sourceMailbox = (MailboxImpl) source;
        if (!sourceMailbox.running.get())
            throw new IllegalStateException(
                    "A valid source mailbox can not be idle");
        final Message message = inbox.createMessage(sourceMailbox, targetActor,
                sourceMailbox.currentMessage, request,
                sourceMailbox.exceptionHandler,
                EventResponseProcessor.SINGLETON);
        addMessage(sourceMailbox, message, this == source);
    }

    @Override
    public final <E, A extends Actor> void send(final _Request<E, A> request,
            final Mailbox source, final A targetActor,
            final ResponseProcessor<E> responseProcessor) throws Exception {
        final MailboxImpl sourceMailbox = (MailboxImpl) source;
        if (!sourceMailbox.running.get())
            throw new IllegalStateException(
                    "A valid source mailbox can not be idle");
        final Message message = inbox.createMessage(sourceMailbox, targetActor,
                sourceMailbox.currentMessage, request,
                sourceMailbox.exceptionHandler, responseProcessor);
        addMessage(sourceMailbox, message, this == source);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <E, A extends Actor> E call(final _Request<E, A> request,
            final A targetActor) throws Exception {
        final Caller caller = new Caller();
        final Message message = inbox.createMessage(caller, targetActor, null,
                request, null,
                (ResponseProcessor<E>) DummyResponseProcessor.SINGLETON);
        // Using a Caller means never local
        // Should we buffer here? (We don't atm) Buffering would be pointless!
        // What if another actor with the same mailbox is called by accident?
        // Don't we get a deadlock?  Yes. And developers can write infinite loops, too.
        // Sanity checks, if you add them, should be turned off in production.
        addMessage(null, message, false);
        return (E) caller.call();
    }

    @Override
    public final ExceptionHandler setExceptionHandler(
            final ExceptionHandler handler) {
        if (!running.get())
            throw new IllegalStateException(
                    "Attempt to set an exception handler on an idle mailbox");
        final ExceptionHandler rv = this.exceptionHandler;
        this.exceptionHandler = handler;
        return rv;
    }

    private void addMessage(final MailboxImpl sourceMailbox,
            final Message message, final boolean local) throws Exception {
        // sourceMailbox is either null, or running ...
        if ((sourceMailbox == null) || local
                || !sourceMailbox.buffer(message, this)) {
            addUnbufferedMessage(message, local);
        }
    }

    /**
     * Adds a message to the queue.
     */
    private void addUnbufferedMessage(final Message message, final boolean local)
            throws Exception {
        inbox.offer(local, message);
        afterAdd();
    }

    /**
     * Adds messages to the queue.
     */
    @Override
    public void addUnbufferedMessages(final Collection<Message> messages)
            throws Exception {
        inbox.offer(messages);
        afterAdd();
    }

    /**
     * Should be called after adding some message(s) to the queue.
     */
    private void afterAdd() throws Exception {
        if (!running.get() && running.compareAndSet(false, true)) {   //strange looking speed enhancement --b
            if (inbox.isNonEmpty())
                mailboxFactory.submit(this);
            else
                running.set(false);
        } else if (messageProcessor != null)
            messageProcessor.run();
    }

    /**
     * Returns true, if the message could be buffered before sending.
     *
     * @param message Message to send-buffer
     * @return true, if buffered
     */
    @Override
    public boolean buffer(final Message message, final MessageSource target) {
        List<Message> buffer;
        if (sendBuffer == null) {
            sendBuffer = new IdentityHashMap<MessageSource, List<Message>>();
            buffer = null;
        } else {
            buffer = sendBuffer.get(target);
        }
        if (buffer == null) {
            buffer = new ArrayList<Message>(initialBufferSize);
            sendBuffer.put(target, buffer);
        }
        buffer.add(message);
        return true;
    }

    @Override
    public final void run() {
        if (messageProcessor != null)
            while (true) {
                final Message message = inbox.poll();
                if (message == null)
                    if (onIdle())
                        return;
                if (message.isResponsePending())
                    processRequestMessage(message);
                else
                    processResponseMessage(message);
            }
        else
            while (true) {
                final Message message = inbox.poll();
                if (message == null) {
                    if (!onIdle())
                        continue;
                    running.set(false);
                    // If inbox.isNonEmpty() was ever to throw an Exception,
                    // we should still be in a consistent state, since there
                    // was no unprocessed message, and running was set to false.
                    if (inbox.isNonEmpty()) {
                        if (!running.compareAndSet(false, true))
                            return;
                        continue;
                    }
                    return;
                }
                if (message.isResponsePending())
                    processRequestMessage(message);
                else
                    processResponseMessage(message);
            }
    }

    /** Called when all pending messages have been processed. */
    private boolean onIdle() {
        try {
            flush();
        } catch (final Throwable t) {
            // processThrowable(t); //do not call processThrowable here //todo: log it
        }
        if (onIdle != null) {
            onIdle.run();
            return !inbox.isNonEmpty();
        }
        return true;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processRequestMessage(final Message message) {
        exceptionHandler = null; //NOPMD
        currentMessage = message;
        final _Request<?, Actor> request = message.getRequest();
        try {
            request.processRequest(message.getTargetActor(),
                    new ResponseProcessor() {
                        @Override
                        public void processResponse(final Object response)
                                throws Exception {
                            if (!message.isResponsePending())
                                return;
                            if (message.getResponseProcessor() != EventResponseProcessor.SINGLETON) {
                                message.setResponse(response);
                                message.getMessageSource().incomingResponse(
                                        message, MailboxImpl.this);
                            } else {
                                //todo: clear Message to speed gc?  No effect!
                                if (response instanceof Throwable) {
                                    log.warn("Uncaught throwable",
                                            (Throwable) response);
                                }
                            }
                        }
                    });
        } catch (final Throwable t) {
            processThrowable(t);
        }
    }

    private void processThrowable(final Throwable t) {
        if (!currentMessage.isResponsePending())
            return;
        final Message message = currentMessage;
        if (exceptionHandler != null) {
            try {
                exceptionHandler.processException(t);
            } catch (final Throwable u) {
                log.error("Exception handler unable to process throwable "
                        + exceptionHandler.getClass().getName(), t);
                if (!(message.getResponseProcessor() instanceof EventResponseProcessor)) {
                    if (!message.isResponsePending())
                        return;
                    currentMessage.setResponse(u);
                    message.getMessageSource().incomingResponse(message,
                            MailboxImpl.this);
                } else {
                    log.error("Thrown by exception handler and uncaught "
                            + exceptionHandler.getClass().getName(), t);
                }
            }
        } else {
            if (!message.isResponsePending())
                return;
            currentMessage.setResponse(t);
            if (!(message.getResponseProcessor() instanceof EventResponseProcessor))
                message.getMessageSource().incomingResponse(message,
                        MailboxImpl.this);
            else {
                log.warn("Uncaught throwable", t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processResponseMessage(final Message message) {
        final Object response = message.getResponse();
        exceptionHandler = message.getSourceExceptionHandler();
        currentMessage = message.getOldMessage();
        if (response instanceof Throwable) {
            processThrowable((Throwable) response);
            return;
        }
        @SuppressWarnings("rawtypes")
        final ResponseProcessor responseProcessor = message
                .getResponseProcessor();
        //todo: clear Message to speed gc?  No effect!
        try {
            responseProcessor.processResponse(response);
        } catch (final Throwable t) {
            processThrowable(t);
        }
    }

    @Override
    public final void incomingResponse(final Message message,
            final Mailbox responseSource) {
//        final MailboxImpl sourceMailbox = (MailboxImpl) responseSource;
//        if (!sourceMailbox.running.get())
//            throw new IllegalStateException(
//                    "A valid source mailbox can not be idle");
        try {
            addMessage(null, message, this == responseSource);
        } catch (final Throwable t) {
            log.error("unable to add response message", t);
        }
    }

    @Override
    public final MailboxFactory getMailboxFactory() {
        return mailboxFactory;
    }
}
