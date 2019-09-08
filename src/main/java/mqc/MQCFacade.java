package mqc;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;

/**
 * @author xp
 */
@RequiredArgsConstructor
public class MQCFacade<T> implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MQCFacade.class);

    private Queue<T> messageBuffer = new ConcurrentLinkedDeque<>();

    private final MQBroker<T> broker;

    private final MQConsumer<T> consumer;

    private T receive() {
        if (messageBuffer.isEmpty()) {
            try {
                Collection<T> messages = broker.receive();
                if (messages != null && messages.size() > 0) {
                    for (T message : messages) {
                        messageBuffer.offer(message);
                    }
                }
            } catch (Throwable e) {
                if (broker.onReceiveError(e)) {
                    return null;
                }
                throw e;
            }
        }
        return messageBuffer.poll();
    }

    private boolean handle(T message, Consumer<T> handler, Predicate<MessageExceptionPair> errorHandler) {
        try {
            handler.accept(message);
            return true;
        } catch (Throwable e) {
            if (errorHandler != null) {
                MessageExceptionPair exceptionWrapper = new MessageExceptionPair(message, e);
                return errorHandler.test(exceptionWrapper);
            }
            throw e;
        }
    }

    private boolean consume(T message) {
        return handle(message, consumer::consume, consumer::onConsumeError);
    }

    private boolean acknowledge(T message) {
        if (message == null) {
            // no message to ack
            return true;
        }
        return handle(message, broker::acknowledge, null);
    }

    private boolean requeue(T message) {
        if (message == null) {
            // no message to requeue
            return true;
        }
        return handle(message, broker::requeue, null);
    }

    @Override
    public void run() {
        try {
            T message = receive();
            if (message == null) {
                if (consumer.isEmptyMessageConcerned()) {
                    consume(null);
                }
            } else {
                if (consume(message)) {
                    acknowledge(message);
                } else {
                    requeue(message);
                }
            }
        } catch (Throwable e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
