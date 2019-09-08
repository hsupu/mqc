package mqc;

import java.util.*;

/**
 * @author xp
 */
public interface MQBroker<T> {

    Collection<T> receive();

    /**
     * @return true to continue, false to close
     */
    boolean onReceiveError(Throwable e);

    /**
     * for message consumed
     */
    void acknowledge(T message);

    /**
     * for message need to re-consume
     */
    void requeue(T message);
}
