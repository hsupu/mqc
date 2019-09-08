package mqc;

import lombok.Data;

/**
 * @author xp
 */
@Data
public class MessageExceptionPair {

    private final Object message;

    private final Throwable exception;
}
