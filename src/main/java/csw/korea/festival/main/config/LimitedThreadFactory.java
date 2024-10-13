package csw.korea.festival.main.config;

import org.jetbrains.annotations.NotNull;

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
    public Thread newThread(@NotNull Runnable r) {
        Runnable wrappedRunnable = () -> {
            boolean permitAcquired = false; // TODO: or tryAcquire?
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