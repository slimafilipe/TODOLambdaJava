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
import dev.filipe.TODOLambdaJava.util.AuthUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ListTasksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final TaskRepository taskRepository;

    public ListTasksHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        String TABLE_NAME = System.getenv("TASKS_TABLE");
        DynamoDbTable<Task> taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));

        this.taskRepository = new TaskRepository(taskTable);
    }

    // Construtor para injeção de dependência em testes
    public ListTasksHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        var logger = context.getLogger();
        logger.log("Recebida requisão para listar tarefas: " + input.getBody());
        try {
           // Map<String, String> querystringParameters = input.getQueryStringParameters();
           // String userId = querystringParameters.get("userId");
            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado.");
            }
            String userId = userIdOpt.get();
            String userPK = "USER#" + userId;
            List<Task> tasks = taskRepository.listTasks(userPK);

            return ApiResponseBuilder.createSuccessResponse(200, tasks);
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
