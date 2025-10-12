package dev.filipe.TODOLambdaJava.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;

import java.util.Map;
import java.util.Optional;

public class DeleteTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final TaskRepository taskRepository;

    public DeleteTaskHandler() {
        this.taskRepository = DependencyFactory.getTaskRepository();
    }
    public DeleteTaskHandler(TaskRepository taskRepository){
        this.taskRepository = taskRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Requisição para deletar tarefa recebida com sucesso.");
        try{

            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado.");
            }
            String userId = userIdOpt.get();
            String userPK = Constants.USER_PREFIX + userId;


            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("taskId")){
                return ApiResponseBuilder.createErrorResponse(400, "taskId é obrigatório.");
            }
            String taskId = input.getPathParameters().get("taskId");
            Optional<Task> existingTasksOptional = taskRepository.findTaskById(userPK, taskId);
            if (existingTasksOptional.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(404, "Tarefa não encontrada.");
            }
            Task taskToDelete = existingTasksOptional.get();

            taskRepository.delete(taskToDelete);
            logger.log("Tarefa excluída com sucesso.");
            return ApiResponseBuilder.createSuccessResponse(204, "Tarefa excluida com sucesso.");
        }catch (Exception e){
            logger.log("Erro ao excluir tarefa: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro no servidor interno.");
        }
    }
}
