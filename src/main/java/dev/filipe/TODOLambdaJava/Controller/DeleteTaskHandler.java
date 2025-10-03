package dev.filipe.TODOLambdaJava.Controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import dev.filipe.TODOLambdaJava.Config.DependecyFactory;
import dev.filipe.TODOLambdaJava.Model.Task;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;

import java.util.Map;
import java.util.Optional;

public class DeleteTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final TaskRepository taskRepository;

    public DeleteTaskHandler() {
        this.taskRepository = DependecyFactory.getTaskRepositoryInstance();
    }
    public DeleteTaskHandler(TaskRepository taskRepository){
        this.taskRepository = taskRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Requisição para deletar tarefa recebida com sucesso.");
        try{
            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("taskId")){
                return ApiResponseBuilder.createErrorResponse(400, "taskId é obrigatório.");
            }
            String userId = "user-id-123";
            String taskId = input.getPathParameters().get("taskId");
            Optional<Task> existingTasksOptional = taskRepository.findTaskById(userId, taskId);
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
