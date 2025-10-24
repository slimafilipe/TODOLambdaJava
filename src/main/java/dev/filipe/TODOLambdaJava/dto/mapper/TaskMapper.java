package dev.filipe.TODOLambdaJava.dto.mapper;

import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.dto.TaskResponseDTO;

public class TaskMapper {

    public TaskMapper() {}

    public static TaskResponseDTO toResponseDTO(Task task){
        if (task == null || task.getTaskId() == null){
            return null;
        }
        String fullSK = task.getTaskId();
        String cleanTaskId = fullSK.substring(fullSK.lastIndexOf("#") + 1);

        return new TaskResponseDTO(
                cleanTaskId,
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getCreatedAt()
        );
    }
}
