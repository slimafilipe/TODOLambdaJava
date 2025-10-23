package dev.filipe.TODOLambdaJava.controller.task;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.dto.TaskResponseDTO;
import dev.filipe.TODOLambdaJava.dto.mapper.TaskMapper;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;

import java.util.Map;
import java.util.Optional;

public class UpdateTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final TaskRepository taskRepository;


    public UpdateTaskHandler(){
        this.taskRepository = DependencyFactory.getTaskRepository();
    }

    public UpdateTaskHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisão para atualizar tarefa: " + input.getBody());
        try{

            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado.");
            }
            String userId = userIdOpt.get();

            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("taskId")){
              return  ApiResponseBuilder.createErrorResponse(400, "taskId obrigatório");
            }
            String taskId = pathParameters.get("taskId");

            Optional<Task> existingTaskOptional = taskRepository.findTaskById(userId, taskId);
            if (existingTaskOptional.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(400, "Tarefa não encontrada");
            }
            Task existingTask = existingTaskOptional.get();

            String requestBody = input.getBody();
            Task updateTaskData = gson.fromJson(requestBody, Task.class);
            existingTask.setTitle(updateTaskData.getTitle());
            existingTask.setDescription(updateTaskData.getDescription());
            existingTask.setCompleted(updateTaskData.isCompleted());
            taskRepository.save(existingTask);

            TaskResponseDTO responseDTO = TaskMapper.toResponseDTO(existingTask);
            logger.log("Tarefa atualizada com sucesso " + taskId);

            return ApiResponseBuilder.createSuccessResponse(200, responseDTO);
        }catch (JsonSyntaxException e){
            logger.log("Erro ao processar requisão" +  e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição inválido");
        }catch (Exception e){
            logger.log("Erro ao atualizar tarefa " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno no servidor");
        }
    }
}
