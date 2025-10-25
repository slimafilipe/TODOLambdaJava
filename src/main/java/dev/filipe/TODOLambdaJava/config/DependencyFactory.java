package dev.filipe.TODOLambdaJava.config;

import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.model.TaskList;
import dev.filipe.TODOLambdaJava.repository.TaskListRepository;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DependencyFactory {
    private static final TaskRepository taskRepositoryInstance;
    private static final TaskListRepository taskListRepository;
    private static final SqsClient sqsClientInstance;

    static {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        sqsClientInstance = SqsClient.builder().build();
        String TABLE_NAME = System.getenv("TASKS_TABLE");
        DynamoDbTable<Task> taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));
        DynamoDbTable<TaskList> taskListTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(TaskList.class));
        taskRepositoryInstance = new TaskRepository(taskTable);
        taskListRepository = new TaskListRepository(taskListTable);

    }

    private DependencyFactory() {}

    public static TaskListRepository getTaskListRepository(){ return taskListRepository; }
    public static TaskRepository getTaskRepository(){
        return taskRepositoryInstance;
    }
    public static SqsClient getSqsClient(){
        return sqsClientInstance;
    }

}
