package dev.filipe.TODOLambdaJava.controller;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import dev.filipe.TODOLambdaJava.dto.TaskResponseDTO;
import dev.filipe.TODOLambdaJava.dto.mapper.TaskMapper;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetTaskByIdHandlerTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    TaskRepository taskRepository;

    private GetTaskByIdHandler getTaskByIdHandler;

    private Gson gson = new Gson();

    @BeforeEach
    void setUp(){
        getTaskByIdHandler = new GetTaskByIdHandler(taskRepository);

        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldReturn200WithGetTaskByIdForAuthenticadedUser(){

        String cognitoUser = UUID.randomUUID().toString();
        String taskId = UUID.randomUUID().toString();

        Task taskFromDb = new Task();
        taskFromDb.setUserId(Constants.USER_PREFIX + cognitoUser);
        taskFromDb.setTaskId(Constants.TASK_PREFIX + taskId);
        taskFromDb.setTitle("Tafera teste");
        taskFromDb.setDescription("Tarefa de teste");

        when(taskRepository.findTaskById(cognitoUser, taskId)).thenReturn(Optional.of(taskFromDb));

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        Map<String, Object> authorized = Map.of("claims", Map.of("sub", cognitoUser));
        requestContext.setAuthorizer(authorized);
        request.setRequestContext(requestContext);
        request.setPathParameters(Map.of("taskId", taskId));
        request.setBody(gson.toJson(taskFromDb));

        APIGatewayProxyResponseEvent response = getTaskByIdHandler.handleRequest(request,context);
        assertEquals(200, response.getStatusCode());

        TaskResponseDTO responseDTO = TaskMapper.toResponseDTO(taskFromDb);
        assertEquals(gson.toJson(responseDTO), response.getBody());

        verify(taskRepository).findTaskById(cognitoUser, taskId);
    }
}
