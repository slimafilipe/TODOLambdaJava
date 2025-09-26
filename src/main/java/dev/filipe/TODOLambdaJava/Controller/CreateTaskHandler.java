package dev.filipe.TODOLambdaJava.Controller;

import dev.filipe.TODOLambdaJava.Model.Task;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;




public class CreateTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson;

    private final DynamoDbTable<Task> taskTable ;

    public CreateTaskHandler(){
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        String TABLE_NAME = System.getenv("TASKS_TABLE");
        this.taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));
        this.gson = new Gson();
    }

    // Construtor para injeção de dependência em testes
    public CreateTaskHandler(DynamoDbTable<Task> taskTable, Gson gson) {
        this.taskTable = taskTable;
        this.gson = gson;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisão para cria tarefa: " + input.getBody());
        try {
            String requestBody = input.getBody();
            if (requestBody == null || requestBody.isEmpty()) {
                logger.log("Corpo da requisição está vazio");
                return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição está vazio");
            }
            Task task = gson.fromJson(requestBody, Task.class);
            task.setUserId("user-id-123");
            task.setTaskId(UUID.randomUUID().toString());
            task.setCreatedAt(Instant.now().toString());
            task.setCompleted(false);

            taskTable.putItem(task);
            logger.log("Tarefa criada com sucesso com ID: " + task.getTaskId());

          //  String responseBody = gson.toJson(task);

            return ApiResponseBuilder.createSuccessResponse(201, gson.toJson(task));
        } catch (JsonSyntaxException e) {
            logger.log("Erro ao processar JSON: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Corpo da requisição inválido");
        } catch (Exception e) {
            logger.log("Erro ao criar tarefa: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
