package taskmaster;

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xp
 */
public class Master {

    private static final Logger LOG = LoggerFactory.getLogger(Master.class);

    private final ExecutorService executorService;

    private final Object lock = new Object();

    private final Set<Worker> workers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public Master(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Worker addWorker(Runnable task) throws InterruptedException {
        if (this.executorService.isShutdown()) {
            throw new IllegalStateException("service is closed");
        }
        synchronized (this.lock) {
            WorkerImpl worker = new WorkerImpl(this);
            worker.setTask(task);
            this.executorService.execute(worker);
            worker.awaitReady();
            return worker;
        }
    }

    public void startAll() throws InterruptedException {
        if (this.executorService.isShutdown()) {
            throw new IllegalStateException("service is closed");
        }
        synchronized (this.lock) {
            for (Worker e : this.workers) {
                e.start();
            }
        }
    }

    public void stopAll() throws InterruptedException {
        if (this.executorService.isShutdown()) {
            throw new IllegalStateException("service is closed");
        }
        synchronized (this.lock) {
            for (Worker e : this.workers) {
                e.stop();
            }
        }
    }

    public void close() throws InterruptedException {
        if (!this.executorService.isShutdown()) {
            this.executorService.shutdown();
        }
        synchronized (this.lock) {
            for (Worker e : this.workers) {
                e.close();
            }
        }
    }

    public void awaitClosed() throws InterruptedException {
        if (!this.executorService.isShutdown()) {
            throw new IllegalStateException("service is running");
        }
        while (!this.executorService.isTerminated()) {
            this.executorService.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    void onWorkerStart(Worker worker) {
        workers.add(worker);
    }

    void onWorkerEnd(Worker worker) {
        workers.remove(worker);
    }
}
