package org.agilewiki.pamailbox.exceptions;

import org.agilewiki.pactor.ExceptionHandler;
import org.agilewiki.pactor.Mailbox;
import org.agilewiki.pactor.Request;
import org.agilewiki.pactor.RequestBase;
import org.agilewiki.pactor.ResponseProcessor;

public class ActorC {
    private final Mailbox mailbox;
    public final Request<String> throwRequest;

    public ActorC(final Mailbox mbox) {
        this.mailbox = mbox;

        throwRequest = new RequestBase<String>(mailbox) {
            @Override
            public void processRequest(
                    final ResponseProcessor<String> responseProcessor)
                    throws Exception {
                mailbox.setExceptionHandler(new ExceptionHandler() {
                    @Override
                    public void processException(final Throwable throwable)
                            throws Exception {
                        responseProcessor.processResponse(throwable.toString());
                    }
                });
                throw new SecurityException("thrown on request");
            }
        };
    }
}
