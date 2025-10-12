package dev.filipe.TODOLambdaJava.Controller;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.filipe.TODOLambdaJava.Model.Task;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;
import java.util.Optional;

public class UpdateTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final TaskRepository taskRepository;


    public UpdateTaskHandler(){
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        String TABLE_NAME = System.getenv("TASKS_TABLE");
        DynamoDbTable<Task> taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));
        this.taskRepository = new TaskRepository(taskTable);
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
            String userPK = "USER#" + userId;

            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("taskId")){
              return  ApiResponseBuilder.createErrorResponse(400, "taskId obrigatório");
            }
            String taskId = input.getPathParameters().get("taskId");

            Optional<Task> existingTaskOptional = taskRepository.findTaskById(userPK, taskId);
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
            logger.log("Tarefa atualizada com sucesso " + taskId);

            return ApiResponseBuilder.createSuccessResponse(200, existingTask);
        }catch (JsonSyntaxException e){
            logger.log("Erro ao processar requisão" +  e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição inválido");
        }catch (Exception e){
            logger.log("Erro ao atualizar tarefa " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno no servidor");
        }
    }
}
