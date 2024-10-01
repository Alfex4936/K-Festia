package csw.korea.festival.main.config;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

public class LimitedThreadFactory implements ThreadFactory {
    private final ThreadFactory delegate;
    private final Semaphore semaphore;

    public LimitedThreadFactory(ThreadFactory delegate, int maxConcurrency) {
        this.delegate = delegate;
        this.semaphore = new Semaphore(maxConcurrency);
    }

    @Override
    public Thread newThread(Runnable r) {
        Runnable wrappedRunnable = () -> {
            boolean permitAcquired = false;
            try {
                semaphore.acquire();
                permitAcquired = true;
                r.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (permitAcquired) {
                    semaphore.release();
                }
            }
        };
        return delegate.newThread(wrappedRunnable);
    }
}