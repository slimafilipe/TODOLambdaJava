package dev.filipe.TODOLambdaJava.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import dev.filipe.TODOLambdaJava.Controller.CreateTaskHandler;
import dev.filipe.TODOLambdaJava.Controller.ListTasksHandler;
import dev.filipe.TODOLambdaJava.Model.Task;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListTaskHandlerTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> taskTable;

    @Mock
    private TaskRepository taskRepository;

    private ListTasksHandler listTasksHandler ;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        listTasksHandler = new ListTasksHandler(taskRepository);


        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldReturn200WithListOfTasksForAuthenticatedUser() {

        String cognitoUserId = UUID.randomUUID().toString();
        String userPartitionKey = "USER#" + cognitoUserId;

        Task inputTask = new Task();
        inputTask.setTitle("Testando task");
        inputTask.setDescription("Este é um teste");

        List<Task> listTasks = List.of(inputTask);

        when(taskRepository.listTasks(userPartitionKey)).thenReturn(listTasks);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        Map<String, Object> authorizer = Map.of("claims", Map.of("sub", cognitoUserId));
        requestContext.setAuthorizer(authorizer);
        request.setRequestContext(requestContext);


        APIGatewayProxyResponseEvent response = listTasksHandler.handleRequest(request, context);
        assertEquals(200, response.getStatusCode());
        assertEquals(gson.toJson(listTasks), response.getBody());


        verify(taskRepository).listTasks(userPartitionKey);

    }

}
