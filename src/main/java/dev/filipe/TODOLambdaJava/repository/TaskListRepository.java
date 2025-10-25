package dev.filipe.TODOLambdaJava.repository;

import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.model.TaskList;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskListRepository {

    private final DynamoDbTable<TaskList> taskListTable;

    public TaskListRepository(DynamoDbTable<TaskList> taskListTable) {
        this.taskListTable = taskListTable;
    }

    public List<TaskList> listTaskLists(String userId) {
        String userPartitionKey = Constants.USER_PREFIX + userId;
        QueryConditional conditional =  QueryConditional.sortBeginsWith(
                Key.builder()
                        .partitionValue(userPartitionKey)
                        .sortValue(Constants.LIST_PREFIX)
                        .build());


        return taskListTable.query(conditional)
                .items()
                .stream()
                .filter(taskList -> taskList.getTaskListId() != null && !taskList.getTaskListId().contains("#" + Constants.TASK_PREFIX))
                .collect(Collectors.toList());
    }

    public Optional<TaskList> findTaskListById(String userId, String taskListId) {
        String userPK = Constants.USER_PREFIX + userId;
        String taskListSK = Constants.LIST_PREFIX + taskListId;

        Key key = Key.builder()
                .partitionValue(userPK)
                .sortValue(taskListSK)
                .build();
        return Optional.ofNullable(taskListTable.getItem(key));
    }

    public void save(TaskList taskList){
        taskListTable.putItem(taskList);
    }

    public void delete(TaskList taskList){
        taskListTable.deleteItem(taskList);
    }
}
