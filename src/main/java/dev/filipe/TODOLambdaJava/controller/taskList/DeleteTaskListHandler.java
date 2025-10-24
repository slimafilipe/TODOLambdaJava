package dev.filipe.TODOLambdaJava.controller.taskList;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.model.TaskList;
import dev.filipe.TODOLambdaJava.repository.TaskListRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;

import java.util.Map;
import java.util.Optional;

public class DeleteTaskListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final TaskListRepository taskListRepository;

    public DeleteTaskListHandler(){this.taskListRepository = DependencyFactory.getTaskListRepository();}
    public DeleteTaskListHandler(TaskListRepository taskListRepository){this.taskListRepository = taskListRepository;}

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisição para deletar lista");

        try {
            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado");
            }
            String userId = userIdOpt.get();

            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters.isEmpty() || !pathParameters.containsKey("listId")){
                return ApiResponseBuilder.createErrorResponse(400, "listId obrigatório!");
            }
            String listId = pathParameters.get("listId");

            Optional<TaskList> existingTaskListOptional = taskListRepository.findTaskListById(userId, listId);
            if (existingTaskListOptional.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(400, "Lista não encontrada!");
            }
            TaskList listToDelete = existingTaskListOptional.get();
            taskListRepository.delete(listToDelete);
            logger.log("Lista removida com sucesso!");
            return ApiResponseBuilder.createSuccessResponse(204, "Lista removida com sucesso.");
        }catch (Exception e){
            logger.log("Erro ao excluir tarefa: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro no servidor interno.");
        }
    }
}
