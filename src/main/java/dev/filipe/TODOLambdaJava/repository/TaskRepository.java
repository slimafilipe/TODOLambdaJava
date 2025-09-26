package dev.filipe.TODOLambdaJava.repository;

import dev.filipe.TODOLambdaJava.Model.Task;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;

public class TaskRepository {
    private static DynamoDbTable<Task> taskTable;;
    // Construtor para injeção de dependência em testes
    public TaskRepository(DynamoDbTable<Task> taskTable) {
        TaskRepository.taskTable = taskTable;
    }

    public static List<Task> listTasks(String userId) {
        QueryConditional conditional =  QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());

        return taskTable.query(conditional).items().stream().toList();
    }
}
