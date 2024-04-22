package com.sheldon.task.demo;

import com.sheldon.task.demo.executor.DemoTaskExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务执行器测试类
 */
@SpringBootTest
public class DemoTaskExecutorTest {

    @Autowired
    private DemoTaskExecutor demoTaskExecutor;

    private ExecutorService randomNumberExecutor;

    private ExecutorService resultWriterExecutor;

    private BlockingQueue<Integer> resultsQueue;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field randomNumberExecutorField = DemoTaskExecutor.class.getDeclaredField("randomNumberExecutor");
        randomNumberExecutorField.setAccessible(true);
        randomNumberExecutor = (ExecutorService) randomNumberExecutorField.get(demoTaskExecutor);

        Field resultWriterExecutorField = DemoTaskExecutor.class.getDeclaredField("resultWriterExecutor");
        resultWriterExecutorField.setAccessible(true);
        resultWriterExecutor = (ExecutorService) resultWriterExecutorField.get(demoTaskExecutor);

        Field resultsQueueField = DemoTaskExecutor.class.getDeclaredField("resultsQueue");
        resultsQueueField.setAccessible(true);
        resultsQueue = (BlockingQueue<Integer>) resultsQueueField.get(demoTaskExecutor);
    }

    @AfterEach
    void tearDown() {
//        demoTaskExecutor.cancelTasks();
    }

    @Test
    void testSubmitTasks() {
        demoTaskExecutor.submitTasks();
        assertFalse(randomNumberExecutor.isShutdown());
        assertFalse(resultWriterExecutor.isShutdown());
    }

    @Test
    void testCancelTasks() throws InterruptedException {
        demoTaskExecutor.submitTasks();
        // 等待任务完成
        Thread.sleep(20000);

        demoTaskExecutor.cancelTasks();
        assertTrue(randomNumberExecutor.isShutdown());
        assertTrue(resultWriterExecutor.isShutdown());
        assertTrue(resultsQueue.isEmpty());
    }

    @Test
    void testCalculateTask() throws InterruptedException {
        demoTaskExecutor.submitTasks();
        // 等待任务完成
        Thread.sleep(20000);

        // 读取result.txt文件，并计算结果个数
        try (Stream<String> lines = Files.lines(Paths.get("result.txt"))) {
            long count = lines.count();
            assertEquals(2000, count);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testWriteResultTask() throws InterruptedException {
        demoTaskExecutor.submitTasks();
        // 等待任务完成
        Thread.sleep(10000);

        demoTaskExecutor.cancelTasks();
        assertTrue(resultsQueue.isEmpty());
    }
}
