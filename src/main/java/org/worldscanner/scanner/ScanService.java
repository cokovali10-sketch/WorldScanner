package org.worldscanner.scanner;

import org.worldscanner.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Сервис для выполнения задач сканирования параллельно.
 */
public final class ScanService {

    private ScanService() {
    }

    public static <T> List<T> runTasks(List<Callable<List<T>>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
        List<T> results = new ArrayList<>();

        try {
            List<Future<List<T>>> futures = executor.invokeAll(tasks);
            for (Future<List<T>> future : futures) {
                try {
                    List<T> taskResult = future.get();
                    if (taskResult != null) {
                        results.addAll(taskResult);
                    }
                } catch (ExecutionException e) {
                    Console.error("Failed to execute scan task: " + e.getCause().getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Console.error("Scan execution was interrupted.");
        } finally {
            executor.shutdown();
        }

        return results;
    }
}
