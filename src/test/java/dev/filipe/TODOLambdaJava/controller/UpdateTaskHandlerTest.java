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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp(){
        updateTaskHandler = new UpdateTaskHandler(taskRepository);

        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldReturn200WithListOfTaskForAuthenticatedUser(){

        String cognitoUserId = UUID.randomUUID().toString();
        String userPartitionKey = "USER#" + cognitoUserId;
        String taskId = UUID.randomUUID().toString();
        String taskSortKey = "TASK#" + taskId;

        Task existingTask = new Task();
        existingTask.setUserId(userPartitionKey);
        existingTask.setTaskId(taskSortKey);
        existingTask.setTitle("Task já existente");
        existingTask.setDescription("Este é um teste");
        existingTask.setCompleted(false);

        Task updateTask = new Task();
        updateTask.setTitle("Titulo novo");
        updateTask.setDescription("Novo corpo da tarefa");
        updateTask.setCompleted(true);

        when(taskRepository.findTaskById(anyString(), anyString())).thenReturn(Optional.empty());
        when(taskRepository.findTaskById(userPartitionKey, taskSortKey)).thenReturn(Optional.of(existingTask));

        APIGatewayProxyRequestEvent request= new APIGatewayProxyRequestEvent();
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        Map<String, Object> authorizer = Map.of("claims", Map.of("sub", cognitoUserId));
        requestContext.setAuthorizer(authorizer);
        request.setRequestContext(requestContext);
        request.setPathParameters(Map.of("taskId", taskSortKey));
        request.setBody(gson.toJson(updateTask));

        APIGatewayProxyResponseEvent response = updateTaskHandler.handleRequest(request, context);
        assertEquals(200, response.getStatusCode());

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();

        assertEquals("Titulo novo", savedTask.getTitle());
        assertEquals("Novo corpo da tarefa", savedTask.getDescription());
        assertTrue(savedTask.isCompleted());

        assertEquals(userPartitionKey, savedTask.getUserId());
        assertEquals(taskSortKey, savedTask.getTaskId());
    }

}
