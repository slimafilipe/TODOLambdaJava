package dev.filipe.TODOLambdaJava.dto.mapper;

import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.dto.TaskResponseDTO;

public class TaskMapper {

    public TaskMapper() {}

    public static TaskResponseDTO toResponseDTO(Task task){
        if (task == null){
            return null;
        }
        return new TaskResponseDTO(
                task.getTaskId().replace(Constants.TASK_PREFIX, ""),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getCreatedAt()
        );
    }
}
