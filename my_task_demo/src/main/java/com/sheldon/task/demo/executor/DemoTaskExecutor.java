package com.sheldon.task.demo.executor;

import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class DemoTaskExecutor {

    private final Lock lock = new ReentrantLock();

    private final Condition allTasksComplete = lock.newCondition();

    private final Condition writeComplete = lock.newCondition();

    private final BlockingQueue<Integer> resultsQueue = new LinkedBlockingQueue<>(2000);

    // 提交任务的线程池，使用单线程
    private final ExecutorService submitTaskExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorCompletionService<Integer> completionService = new ExecutorCompletionService<>(submitTaskExecutor);

    // 写文件的线程池，使用单线程
    private final ExecutorService resultWriterExecutor = Executors.newSingleThreadExecutor();

    public void submitTasks() {
        long startTimeMillis = System.currentTimeMillis();
        System.out.println("submitTasks, time=" + startTimeMillis);

        // 提交2000个计算任务
        for (int i = 0; i < 2000; i++) {
            CalculateTask calculateTask = new CalculateTask();
            completionService.submit(calculateTask);
        }
        for (int i = 0; i < 2000; i++) {
            try {
                Future<Integer> future = completionService.take();
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        submitTaskExecutor.shutdown();

        // 任务量为2000，可以等待所有任务完成后再写文件
        resultWriterExecutor.submit(() -> {
            lock.lock();
            try {
                while (resultsQueue.size() < 2000) {
                    allTasksComplete.await();
                }
                new WriteResultTask().run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        // 等待写文件的任务完成后关闭线程池
        lock.lock();
        try {
            while (!resultsQueue.isEmpty()) {
                writeComplete.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        resultWriterExecutor.shutdown();

        System.out.println("submitTasks finish, time=" + System.currentTimeMillis());
        System.out.println("submitTasks cost=" + (System.currentTimeMillis() - startTimeMillis) + "ms");
    }

    public void cancelTasks() {
        lock.lock();
        try {
            submitTaskExecutor.shutdownNow();
            resultWriterExecutor.shutdownNow();
            allTasksComplete.signal();
            // 等待写文件的任务结束后清空队列
            resultWriterExecutor.awaitTermination(10, TimeUnit.SECONDS);
            resultsQueue.clear();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 计算任务
     * 1、生成1万个随机数并计算奇数个数，这个过程会分给10个线程并行去做，每个线程生成1000个随机数并计算奇数个数
     * 2、将所有线程的计算结果累加，最终结果存入一个队列
     */
    private class CalculateTask implements Callable<Integer> {

        private final ExecutorService randomNumberGeneratorExecutor = Executors.newFixedThreadPool(10);
        Future<Integer>[] futures = new Future[10];

        @Override
        public Integer call() {
            try {
                for (int i = 0; i < 10; i++) {
                    futures[i] = randomNumberGeneratorExecutor.submit(this::generateRandomNumbersAndCountOdds);
                }
                int totalOddCount = 0;
                for (int i = 0; i < 10; i++) {
                    totalOddCount += futures[i].get();
                }
                resultsQueue.offer(totalOddCount);
                checkAllTasksComplete();
                return totalOddCount;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } finally {
                randomNumberGeneratorExecutor.shutdown();
            }
            return null;
        }

        /**
         * 生成1000个随机数并计算奇数个数
         */
        private int generateRandomNumbersAndCountOdds() {
            int count = 0;
            Random random = new Random();
            for (int j = 0; j < 1000; j++) {
                int randomNumber = random.nextInt();
                if (randomNumber % 2 != 0) {
                    count++;
                }
            }
            return count;
        }

        /**
         * 检查所有任务是否完成，如果完成则唤醒写文件的任务
         */
        private void checkAllTasksComplete() {
            lock.lock();
            try {
                if (resultsQueue.size() == 2000) {
                    allTasksComplete.signal();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 一次性输出结果到文件的任务
     */
    private class WriteResultTask implements Runnable {
        @Override
        public void run() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("result.txt"))) {
                while (!resultsQueue.isEmpty()) {
                    writer.write(String.valueOf(resultsQueue.poll()));
                    writer.newLine();
                }
                writeComplete.signal();
                System.out.println("write result to file finish");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
