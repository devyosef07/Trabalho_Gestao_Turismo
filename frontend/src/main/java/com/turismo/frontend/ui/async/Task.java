package com.turismo.frontend.ui.async;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Executa trabalho bloqueante (pedidos à API) fora da Event Dispatch Thread
 * e entrega o resultado — ou o erro — de volta na EDT.
 */
public final class Task<T> extends SwingWorker<T, Void> {

    @FunctionalInterface
    public interface Work<T> {
        T call() throws Exception;
    }

    private final Work<T> work;
    private final Consumer<T> onSuccess;
    private final Consumer<Exception> onError;
    private final Runnable onFinally;

    private Task(Work<T> work, Consumer<T> onSuccess, Consumer<Exception> onError, Runnable onFinally) {
        this.work = work;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onFinally = onFinally;
    }

    public static <T> Task<T> run(Work<T> work, Consumer<T> onSuccess, Consumer<Exception> onError) {
        return run(work, onSuccess, onError, null);
    }

    public static <T> Task<T> run(Work<T> work, Consumer<T> onSuccess, Consumer<Exception> onError, Runnable onFinally) {
        Task<T> task = new Task<>(work, onSuccess, onError, onFinally);
        task.execute();
        return task;
    }

    @Override
    protected T doInBackground() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Task executada na EDT");
        }
        return work.call();
    }

    @Override
    protected void done() {
        try {
            if (isCancelled()) {
                return;
            }
            T result;
            try {
                result = get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                onError.accept(cause instanceof Exception ? (Exception) cause : new RuntimeException(cause));
                return;
            } catch (InterruptedException | CancellationException e) {
                return;
            }
            onSuccess.accept(result);
        } finally {
            if (onFinally != null) {
                onFinally.run();
            }
        }
    }
}
