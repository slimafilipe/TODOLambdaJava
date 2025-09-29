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

    public UpdateTaskHandler(DynamoDbTable<Task> taskTable, TaskRepository taskRepository, Gson gson) {
        this.taskRepository = taskRepository;
        this.taskTable = taskTable;
        this.gson = new Gson();
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisão para atualizar tarefa: " + input.getBody());
        try{
            String userId = "user-id-123";
            String taskId = input.getPathParameters().get("taskId");
            String findedTask = gson.toJson(taskRepository.findTaskById(userId, taskId));
            logger.log("Tarefa encontrada: " + findedTask);
            String requestBody = input.getBody();
            if (requestBody == null || requestBody.isEmpty()){
                logger.log("Corpo da requisição está vázio:");
                return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição está vázio");
            }
            Task task = gson.fromJson(requestBody, Task.class);
            taskTable.putItem(task);

            return ApiResponseBuilder.createSuccessResponse(200, "Tarefa editada com sucesso");
        }catch (JsonSyntaxException e){
            logger.log("Erro ao processar requisão" +  e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição inválido");
        }catch (Exception e){
            logger.log("Erro ao atualizar tarefa " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno no servidor");
        }
    }
}
