package dev.filipe.TODOLambdaJava.controller.task;

import com.google.gson.JsonSyntaxException;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.model.Task;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import dev.filipe.TODOLambdaJava.dto.TaskResponseDTO;
import dev.filipe.TODOLambdaJava.dto.mapper.TaskMapper;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;

import java.util.List;
import java.util.Optional;

public class ListTasksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final TaskRepository taskRepository;

    public ListTasksHandler() {
        this.taskRepository = DependencyFactory.getTaskRepository();
    }

    // Construtor para injeção de dependência em testes
    public ListTasksHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        var logger = context.getLogger();
        logger.log("Recebida requisão para listar tarefas: " + input.getBody());
        try {
           // Map<String, String> querystringParameters = input.getQueryStringParameters();
           // String userId = querystringParameters.get("userId");
            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado.");
            }
            String userId = userIdOpt.get();
            String userPK = Constants.USER_PREFIX + userId;
            List<Task> tasks = taskRepository.listTasks(userPK);

            List<TaskResponseDTO> responseDTOs = tasks.stream()
                    .map(TaskMapper::toResponseDTO)
                    .toList();
            return ApiResponseBuilder.createSuccessResponse(200, responseDTOs);
        } catch (JsonSyntaxException e){
            logger.log("Erro ao construir resposta JSON: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Requisição inválida");
        }
        catch (Exception e) {
            logger.log("Erro ao listar tarefas: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }

    }
}
