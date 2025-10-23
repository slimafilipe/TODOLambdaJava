package dev.filipe.TODOLambdaJava.controller.taskList;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.dto.TaskListResponseDTO;
import dev.filipe.TODOLambdaJava.dto.mapper.TaskListMapper;
import dev.filipe.TODOLambdaJava.model.TaskList;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.repository.TaskListRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;


import java.util.Optional;
import java.util.UUID;

public class CreateListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Gson gson = new Gson();
    private final TaskListRepository taskListRepository;

    public CreateListHandler(){
        this.taskListRepository = DependencyFactory.getTaskListRepository();
    }

    // Construtor para injeção de dependência em testes
    public CreateListHandler(TaskListRepository taskListRepository) {
        this.taskListRepository = taskListRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisição para criar lista de tarefa");

        try {
            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado");
            }
            String userId = userIdOpt.get();

            String requestBody = input.getBody();
            if (requestBody == null || requestBody.isEmpty()) {
                logger.log("Corpo da requisição está vazio");
                return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição está vazio");
            }
            TaskList taskList = gson.fromJson(requestBody, TaskList.class);
            taskList.setUserId(Constants.USER_PREFIX + userId );
            taskList.setTaskListId(Constants.LIST_PREFIX + UUID.randomUUID().toString());
            taskListRepository.save(taskList);
            logger.log("Lista " + taskList.getTaskListId() + "criada com sucesso para o usuario " + userId);
            TaskListResponseDTO responseDTO = TaskListMapper.responseDTO(taskList);

            return ApiResponseBuilder.createSuccessResponse(201, responseDTO);
        } catch (JsonSyntaxException e){
            logger.log("Erro ao processar JSON " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição inválido");
        } catch (Exception e){
            logger.log("Erro ao criar lista " + e.getMessage());
            logger.log("Stack trace: " + e);
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno no servidor");
        }
    }
}
