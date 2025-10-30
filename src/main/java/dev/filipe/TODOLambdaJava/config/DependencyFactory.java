package dev.filipe.TODOLambdaJava.config;

import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.model.TaskList;
import dev.filipe.TODOLambdaJava.repository.TaskListRepository;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DependencyFactory {
    private static final TaskRepository taskRepositoryInstance;
    private static final TaskListRepository taskListRepository;
    private static final SqsClient sqsClientInstance;
    private static final SesClient sesClientInstance;
    private static final S3Client s3ClientInstance;
    private static final S3Presigner s3PresignerInstance;

    static {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        sqsClientInstance = SqsClient.builder().build();
        sesClientInstance = SesClient.builder().build();
        s3ClientInstance = S3Client.builder().build();
        s3PresignerInstance = S3Presigner.builder()
                .region(Region.SA_EAST_1)
                .build();
        String TABLE_NAME = System.getenv("TASKS_TABLE");
        DynamoDbTable<Task> taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Task.class));
        DynamoDbTable<TaskList> taskListTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(TaskList.class));
        taskRepositoryInstance = new TaskRepository(taskTable);
        taskListRepository = new TaskListRepository(taskListTable);

    }

    private DependencyFactory() {}

    public static TaskListRepository getTaskListRepository() { return taskListRepository; }
    public static TaskRepository getTaskRepository(){
        return taskRepositoryInstance;
    }
    public static SqsClient getSqsClient() { return sqsClientInstance; }
    public static S3Client getS3Client() { return s3ClientInstance; }
    public static SesClient getSesClient() { return sesClientInstance; }
    public static S3Presigner getS3Presigner() { return s3PresignerInstance; }



}
