package dev.filipe.TODOLambdaJava.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.dto.TaskResponseDTO;
import dev.filipe.TODOLambdaJava.dto.mapper.TaskMapper;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;

import java.util.Map;
import java.util.Optional;

public class GetTaskByIdHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {


    private final TaskRepository taskRepository;


    public GetTaskByIdHandler(){
        this.taskRepository = DependencyFactory.getTaskRepository();
    }

    public GetTaskByIdHandler(TaskRepository taskRepository){this.taskRepository = taskRepository;}



    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        var logger = context.getLogger();

        try {
            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado.");
            }
            String userId = userIdOpt.get();

            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("taskId")){
                return ApiResponseBuilder.createErrorResponse(400, "taskId obrigatório no caminho da URL");
            }
            String taskId = pathParameters.get("taskId");
            logger.log("Recebida requisição para buscar tarefa com id " + taskId + " do usuario: " + userId);

            Optional<Task> taskOpt = taskRepository.findTaskById(userId, taskId);
            if (taskOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(404, "Tarefa não encontrada");
            }

            TaskResponseDTO responseDTO = TaskMapper.toResponseDTO(taskOpt.get());
            return ApiResponseBuilder.createSuccessResponse(200, responseDTO);

        } catch (Exception e){
            logger.log("Erro no servidor interno" + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro no servidor interno");
        }
    }
}
