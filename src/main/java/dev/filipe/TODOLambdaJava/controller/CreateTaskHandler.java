package dev.filipe.TODOLambdaJava.controller;

import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.model.Task;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dev.filipe.TODOLambdaJava.dto.TaskResponseDTO;
import dev.filipe.TODOLambdaJava.dto.mapper.TaskMapper;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;




public class CreateTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final TaskRepository taskRepository;

    public CreateTaskHandler(){
        this.taskRepository = DependencyFactory.getTaskRepository();
    }

    // Construtor para injeção de dependência em testes
    public CreateTaskHandler(TaskRepository taskRepository) {
       this.taskRepository = taskRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisão para cria tarefa: " + input.getBody());
        try {
            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não encontrado.");
            }
            String userId = userIdOpt.get();

            String requestBody = input.getBody();
            if (requestBody == null || requestBody.isEmpty()) {
                logger.log("Corpo da requisição está vazio");
                return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição está vazio");
            }

            Task task = gson.fromJson(requestBody, Task.class);
            task.setUserId(Constants.USER_PREFIX + userId );
            task.setTaskId(Constants.TASK_PREFIX + UUID.randomUUID().toString());
            task.setCreatedAt(Instant.now().toString());
            task.setCompleted(false);

            taskRepository.save(task);
            logger.log("Tarefa criada com sucesso com ID: " + task.getTaskId());
            TaskResponseDTO responseDTO = TaskMapper.toResponseDTO(task);

            return ApiResponseBuilder.createSuccessResponse(201, responseDTO);
        } catch (JsonSyntaxException e) {
            logger.log("Erro ao processar JSON: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição inválido");
        } catch (Exception e) {
            logger.log("Erro ao criar tarefa: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
