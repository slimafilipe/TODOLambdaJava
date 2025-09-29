package dev.filipe.TODOLambdaJava.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import dev.filipe.TODOLambdaJava.Controller.UpdateTaskHandler;
import dev.filipe.TODOLambdaJava.Model.Task;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateTaskHandlerTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> tasktTable;

    @Mock
    private TaskRepository taskRepository;

    private UpdateTaskHandler updateTaskHandler;

    private Gson gson = new Gson();

    @BeforeEach
    void setUp(){
        updateTaskHandler = new UpdateTaskHandler(tasktTable, gson, taskRepository);

        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void testUpdateHandler(){

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();

        String userId = "user-id-123";

        Task task = new Task();
        task.setTaskId("task-id-456");
        task.setTitle("Task test");
        task.setDescription("Este é um teste");

        String taskIdGet = task.getTaskId();
        request.setBody(gson.toJson(task));

        Optional<Task> findTaskById = Optional.of(task);
        when(taskRepository.findTaskById(userId, taskIdGet)).thenReturn(findTaskById);

        APIGatewayProxyResponseEvent response = updateTaskHandler.handleRequest(request, context);
        assertEquals(200, response.getStatusCode());
        assertEquals(gson.toJson(findTaskById), response.getBody());

        verify(tasktTable).putItem(any(Task.class));
        verify(taskRepository).findTaskById(userId, taskIdGet);
    }

}
