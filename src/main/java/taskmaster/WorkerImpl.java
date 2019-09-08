package taskmaster;

import lombok.Getter;
import lombok.Setter;

/**
 * @author xp
 */
public class WorkerImpl implements Worker, Runnable {

    private enum Op {
        NONE,
        START,
        STOP,
        CLOSE,
    }

    private enum State {
        INIT,
        RUNNING,
        STOPPED,
        CLOSED,
    }

    private final Master master;

    private volatile State state = State.INIT;

    private volatile Op op = Op.NONE;

    private final Object opLock = new Object();

    @Setter
    private Runnable task;
    
    @Getter
    private Throwable caught;

    public WorkerImpl(Master master) {
        this.master = master;
    }

    public boolean isOpNone() {
        return this.op == Op.NONE;
    }

    public boolean isOpClose() {
        return this.op == Op.CLOSE;
    }
    
    public boolean isInit() {
        return this.state == State.INIT;
    }

    public boolean isStopped() {
        return this.state == State.STOPPED;
    }

    public boolean isClosed() {
        return this.state == State.CLOSED;
    }

    public boolean isRunning() {
        return this.state == State.RUNNING;
    }

    private boolean setOp(Op op) throws InterruptedException {
        synchronized (this.opLock) {
            while (!isOpNone()) {
                if (isOpClose()) return false;
                this.opLock.wait();
            }
            this.op = op;
            this.opLock.notifyAll();
            return true;
        }
    }

    @Override
    public void awaitReady() throws InterruptedException {
        if (isInit()) {
            synchronized (this.opLock) {
                while (isInit()) {
                    this.opLock.wait();
                }
            }
        }
    }

    @Override
    public boolean start() throws InterruptedException {
        return !isClosed() && setOp(Op.START);
    }

    @Override
    public boolean stop() throws InterruptedException {
        return !isClosed() && setOp(Op.STOP);
    }

    @Override
    public boolean close() throws InterruptedException {
        return isClosed() || setOp(Op.CLOSE);
    }

    @Override
    public void awaitClosed() throws InterruptedException {
        if (!isClosed() && isOpClose()) {
            throw new IllegalStateException("must close first");
        }
        synchronized (this.opLock) {
            while (!isClosed()) {
                this.opLock.wait();
            }
        }
    }

    @Override
    public void run() {
        master.onWorkerStart(this);
        synchronized (this.opLock) {
            this.state = State.STOPPED;
            this.opLock.notifyAll();
        }

        try {
            run0();
        } catch (Throwable throwable) {
            this.caught = throwable;
        }

        master.onWorkerEnd(this);
        synchronized (this.opLock) {
            this.state = State.CLOSED;
            this.opLock.notifyAll();
        }
    }

    private void run0() throws InterruptedException {
        loop:
        while (!isClosed()) {
            synchronized (this.opLock) {
                switch (this.op) {
                    case NONE:
                        while (isStopped()) {
                            if (isOpNone()) {
                                this.opLock.wait();
                            } else {
                                continue loop;
                            }
                        }
                        break;
                    case START:
                        this.state = State.RUNNING;
                        this.op = Op.NONE;
                        this.opLock.notifyAll();
                        continue loop;
                    case STOP:
                        this.state = State.STOPPED;
                        this.op = Op.NONE;
                        this.opLock.notifyAll();
                        continue loop;
                    case CLOSE:
                        break loop;
                }
            }
            if (isRunning() && task != null) {
                task.run();
            }
        }
    }
}
