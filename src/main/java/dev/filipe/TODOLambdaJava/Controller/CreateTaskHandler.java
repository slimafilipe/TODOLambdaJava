package dev.filipe.TODOLambdaJava.Controller;

import dev.filipe.TODOLambdaJava.Model.Task;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;




public class CreateTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
    private static final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();

    private static final Gson gson = new Gson();

    private static final String TABLE_NAME = System.getenv("TASKS_TABLE");
    private static final DynamoDbTable<Task> taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisão para cria tarefa: " + input.getBody());
        try {
            String requestBody = input.getBody();
            if (requestBody == null || requestBody.isEmpty()) {
                logger.log("Corpo da requisição está vazio");
                return createErrorResponse(400, "Corpo da requisição está vazio");
            }
            Task task = gson.fromJson(requestBody, Task.class);

            task.setUserId(UUID.randomUUID().toString());
            task.setTaskId(UUID.randomUUID().toString());
            task.setCreatedAt(Instant.now().toString());
            task.setCompleted(false);

            taskTable.putItem(task);
            logger.log("Tarefa criada com sucesso com ID: " + task.getTaskId());

            String responseBody = gson.toJson(task);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(responseBody)
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (JsonSyntaxException e) {
            logger.log("Erro ao processar JSON: " + e.getMessage());
            return createErrorResponse(400, "Corpo da requisição inválido");
        } catch (Exception e) {
            logger.log("Erro ao criar tarefa: " + e.getMessage());
            return createErrorResponse(500, "Erro interno do servidor");
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String body) {
        String errorPayload = gson.toJson(Collections.singletonMap("error", body));
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(errorPayload)
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
    }
}
