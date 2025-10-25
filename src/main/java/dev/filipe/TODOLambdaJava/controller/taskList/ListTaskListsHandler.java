package dev.filipe.TODOLambdaJava.controller.taskList;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.filipe.TODOLambdaJava.dto.mapper.TaskListMapper;
import dev.filipe.TODOLambdaJava.model.TaskList;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.dto.TaskListResponseDTO;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.repository.TaskListRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;

import java.util.List;
import java.util.Optional;

public class ListTaskListsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Gson gson = new Gson();
    private final TaskListRepository taskListRepository;

    public ListTaskListsHandler(){this.taskListRepository = DependencyFactory.getTaskListRepository();}

    public ListTaskListsHandler(TaskListRepository taskListRepository){this.taskListRepository = taskListRepository;}


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisição para listar listas ");

        try {
            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado");
            }
            String userId = userIdOpt.get();
            List<TaskList> taskList = taskListRepository.listTaskLists(userId);

            List<TaskListResponseDTO> responseDTOS = taskList.stream()
                    .map(TaskListMapper::responseDTO)
                    .toList();
            return ApiResponseBuilder.createSuccessResponse(201, responseDTOS);

        }catch (JsonSyntaxException e){
            logger.log("Erro ao construir resposta JSON: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Requisição inválida");
        }
        catch (Exception e) {
            logger.log("Erro ao listar tarefas: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
