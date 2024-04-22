package com.sheldon.task.demo.controller;

import com.sheldon.task.demo.executor.DemoTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    private final DemoTaskExecutor demoTaskExecutor;

    public TaskController(DemoTaskExecutor demoTaskExecutor) {
        this.demoTaskExecutor = demoTaskExecutor;
    }

    @GetMapping("/submitTasks")
    public String submitTasks() {
        demoTaskExecutor.submitTasks();
        return "All tasks have been submitted.";
    }

    @GetMapping("/cancelTasks")
    public String cancelTasks() {
        demoTaskExecutor.cancelTasks();
        return "All tasks have been cancelled.";
    }

}
