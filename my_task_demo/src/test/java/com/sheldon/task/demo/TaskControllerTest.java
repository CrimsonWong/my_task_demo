package com.sheldon.task.demo;

import com.sheldon.task.demo.controller.TaskController;
import com.sheldon.task.demo.executor.DemoTaskExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * controller类的单测
 */
public class TaskControllerTest {

    private MockMvc mockMvc;

    private DemoTaskExecutor demoTaskExecutor;

    @BeforeEach
    public void setup() {
        demoTaskExecutor = Mockito.mock(DemoTaskExecutor.class);
        TaskController taskController = new TaskController(demoTaskExecutor);
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
    }

    @Test
    public void testSubmitTasks() throws Exception {
        mockMvc.perform(get("/submitTasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("All tasks have been submitted."));

        Mockito.verify(demoTaskExecutor, Mockito.times(1)).submitTasks();
    }

    @Test
    public void testCancelTasks() throws Exception {
        mockMvc.perform(get("/cancelTasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("All tasks have been cancelled."));

        Mockito.verify(demoTaskExecutor, Mockito.times(1)).cancelTasks();
    }

}
