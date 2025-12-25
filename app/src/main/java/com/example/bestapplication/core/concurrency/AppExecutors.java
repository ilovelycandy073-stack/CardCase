package com.example.bestapplication.core.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Centralized executors for running app work off the main thread.
 *
 * Notes:
 * - Keep it small and dependency-free (plain Java).
 * - Prefer using {@link #io()} for database / file / network work.
 */
public final class AppExecutors {

    private AppExecutors() {}

    // IO: DB / file / network. 2 threads are usually enough for this app.
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);

    public static Executor io() {
        return IO;
    }
}
