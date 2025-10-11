package dev.filipe.TODOLambdaJava.controller;

import dev.filipe.TODOLambdaJava.Controller.CreateTaskHandler;
import dev.filipe.TODOLambdaJava.Model.Task;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.AuthUtils;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import com.google.gson.Gson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateTaskHandlerTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private TaskRepository taskRepository;

    private CreateTaskHandler createTaskHandler ;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        // Usando o construtor que aceita dependências para injeção de mocks
        when(context.getLogger()).thenReturn(logger);
        createTaskHandler = new CreateTaskHandler(taskRepository);
    }

    @Test
    void shoudCreateTaskSucessfully() {
        String cognitoUserId = UUID.randomUUID().toString();

        Task inputTask = new Task();
        inputTask.setTitle("Testando task");
        inputTask.setDescription("Este é um teste");

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(gson.toJson(inputTask));

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        Map<String, Object> authorizer = Map.of("claims", Map.of("sub", cognitoUserId));
        requestContext.setAuthorizer(authorizer);
        request.setRequestContext(requestContext);


        APIGatewayProxyResponseEvent expectedResponse = createTaskHandler.handleRequest(request, context);
        assertEquals(201, expectedResponse.getStatusCode());

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();

        assertEquals("Testando task", savedTask.getTitle());
        assertEquals("Este é um teste", savedTask.getDescription());
        assertEquals("USER#" + cognitoUserId, savedTask.getUserId());
        assertTrue(savedTask.getTaskId().startsWith("TASK#"));
        assertNotNull(savedTask.getCreatedAt());
        assertFalse(savedTask.isCompleted());
    }

}
