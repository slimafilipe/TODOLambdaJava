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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;
import java.util.Optional;

public class UpdateTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson;
    private final TaskRepository taskRepository;
    private final DynamoDbTable<Task> taskTable;

    public UpdateTaskHandler(){
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        String TABLE_NAME = System.getenv("TASKS_TABLE");
        this.taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));
        this.gson = new Gson();
        this.taskRepository = new TaskRepository(taskTable);
    }

    public UpdateTaskHandler( DynamoDbTable<Task> taskTable, Gson gson, TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        this.gson = new Gson();
        this.taskTable =  taskTable;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisão para atualizar tarefa: " + input.getBody());
        try{
            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("taskId")){
                ApiResponseBuilder.createErrorResponse(400, "taskId obrigatório");
            }
            String userId = "user-id-23";
            String taskId = input.getPathParameters().get("taskId");

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
