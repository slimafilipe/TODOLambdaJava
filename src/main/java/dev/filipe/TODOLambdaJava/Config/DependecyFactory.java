package dev.filipe.TODOLambdaJava.Config;

import dev.filipe.TODOLambdaJava.Model.Task;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DependecyFactory {
    private static final TaskRepository taskRepositoryInstance;

    static {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        String TABLE_NAME = System.getenv("TASKS_TABLE");
        DynamoDbTable<Task> taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));
        taskRepositoryInstance = new TaskRepository(taskTable);
    }

    private DependecyFactory() {}

    public static TaskRepository getTaskRepositoryInstance(){
        return taskRepositoryInstance;
    }

}
