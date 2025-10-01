package dev.filipe.TODOLambdaJava.repository;

import dev.filipe.TODOLambdaJava.Model.Task;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskRepository {
    private final DynamoDbTable<Task> taskTable;

    public TaskRepository(DynamoDbTable<Task> taskTable) {
        this.taskTable = taskTable;
    }

    public  List<Task> listTasks(String userId) {
        QueryConditional conditional =  QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());

        return taskTable.query(conditional).items().stream().collect(Collectors.toList());
    }

    public Optional<Task> findTaskById(String userId, String taskId) {
        Key key = Key.builder()
                .partitionValue(userId)
                .sortValue(taskId)
                .build();
        return Optional.ofNullable(taskTable.getItem(key));
    }

    public void save(Task task){
        taskTable.putItem(task);
    }
}
