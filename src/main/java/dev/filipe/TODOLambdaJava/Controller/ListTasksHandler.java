package dev.filipe.TODOLambdaJava.Controller;

import com.google.gson.JsonSyntaxException;
import dev.filipe.TODOLambdaJava.Model.Task;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;
import java.util.List;

public class ListTasksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson;

    private final DynamoDbTable<Task> taskTable ;

    public ListTasksHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        String TABLE_NAME = System.getenv("TASKS_TABLE");
        this.taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));
        this.gson = new Gson();
    }
    // Construtor para injeção de dependência em testes
    public ListTasksHandler(DynamoDbTable<Task> taskTable, Gson gson) {
        this.taskTable = taskTable;
        this.gson = gson;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {

        var logger = context.getLogger();
        logger.log("Recebida requisão para listar tarefas: " + apiGatewayProxyRequestEvent.getBody());
        try {
            String userId = "user-id-123";
            List<Task> tasks = TaskRepository.listTasks(userId);

            return ApiResponseBuilder.createSucessResponse(200, gson.toJson(tasks));
        } catch (JsonSyntaxException e){
            logger.log("Erro ao construir resposta JSON: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Requisição inválida");
        }
        catch (Exception e) {
            logger.log("Erro ao listar tarefas: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }

    }
}
