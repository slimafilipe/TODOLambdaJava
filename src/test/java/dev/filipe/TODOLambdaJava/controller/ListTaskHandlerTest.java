package dev.filipe.TODOLambdaJava.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import dev.filipe.TODOLambdaJava.controller.task.ListTasksHandler;
import dev.filipe.TODOLambdaJava.dto.TaskResponseDTO;
import dev.filipe.TODOLambdaJava.dto.mapper.TaskMapper;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.model.TaskList;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListTaskHandlerTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

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
        String listId = UUID.randomUUID().toString();
        String userPartitionKey = Constants.USER_PREFIX + cognitoUserId;
        String uniqueTaskId =  UUID.randomUUID().toString();
        String taskSortKey = Constants.LIST_PREFIX + listId + "#" + Constants.TASK_PREFIX + uniqueTaskId;

        Task taskFromDb = new Task();
        taskFromDb.setUserId(userPartitionKey);
        taskFromDb.setTaskId(taskSortKey);
        taskFromDb.setTitle("Testando task");
        taskFromDb.setDescription("Este é um teste");
        List<Task> tasksFromRepository = List.of(taskFromDb);

        when(taskRepository.listTasks(cognitoUserId, listId)).thenReturn(tasksFromRepository);

        List<TaskResponseDTO> expectedResponseDtos = tasksFromRepository.stream()
                .map(TaskMapper::toResponseDTO)
                .toList();

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        Map<String, Object> authorizer = Map.of("claims", Map.of("sub", cognitoUserId));
        requestContext.setAuthorizer(authorizer);
        request.setRequestContext(requestContext);
        request.setPathParameters(Map.of("listId", listId));


        APIGatewayProxyResponseEvent response = listTasksHandler.handleRequest(request, context);
        assertEquals(200, response.getStatusCode());
        assertEquals(gson.toJson(expectedResponseDtos), response.getBody());


        verify(taskRepository).listTasks(cognitoUserId, listId);

    }

}
