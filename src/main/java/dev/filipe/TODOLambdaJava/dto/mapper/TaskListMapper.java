package dev.filipe.TODOLambdaJava.dto.mapper;

import dev.filipe.TODOLambdaJava.dto.TaskListResponseDTO;
import dev.filipe.TODOLambdaJava.model.TaskList;
import dev.filipe.TODOLambdaJava.model.constants.Constants;

public class TaskListMapper {

    public TaskListMapper() {
    }

    public static TaskListResponseDTO responseDTO(TaskList taskList){
        if (taskList == null){
            return  null;
        }
        return new  TaskListResponseDTO(
                taskList.getTaskListId().replace(Constants.LIST_PREFIX, ""),
                taskList.getListName()
        );
    }
}
