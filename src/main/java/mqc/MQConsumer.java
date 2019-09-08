package mqc;

/**
 * @author xp
 */
public interface MQConsumer<T> {

    /**
     * @return true if there is no message received and consumer should still be called
     */
    boolean isEmptyMessageConcerned();

    void consume(T message);

    /**
     * @return true to continue (do ack), false to throw and close
     */
    boolean onConsumeError(MessageExceptionPair pair);
}
