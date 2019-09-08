package taskmaster;

/**
 * @author xp
 */
public interface Worker {

    /**
     * ready means worker can be controlled (start, stop, close)
     */
    void awaitReady() throws InterruptedException;

    boolean start() throws InterruptedException;

    boolean stop() throws InterruptedException;

    /**
     * once worker is closed, it's never open again
     */
    boolean close() throws InterruptedException;

    void awaitClosed() throws InterruptedException;
}
