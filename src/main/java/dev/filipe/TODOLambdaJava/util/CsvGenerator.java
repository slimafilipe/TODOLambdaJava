package dev.filipe.TODOLambdaJava.util;

import com.opencsv.CSVWriter;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.model.constants.Constants;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class CsvGenerator {

    public static String generateHierarchicalCsv(List<Task> taskList, Map<String, List<Task>> taskByListId){
        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)){
            String[] header = {
                    "Tipo de Item", "ID da Lista", "Nome da lista",
                    "ID da Tarefa", "Titulo da Tarefa", "Descrição", "Concluída"
            };
            csvWriter.writeNext(header);

            for (Task list : taskList){
                String listId = list.getTaskId().replace(Constants.LIST_PREFIX, "");

                String[] listLine = {
                        "Lista",
                        listId,
                        list.getTitle(),
                        "","","",""
                };
                csvWriter.writeNext(listLine);

                List<Task> taskInsThisList = taskByListId.getOrDefault(list.getTaskId(), List.of());
                for (Task task : taskInsThisList) {
                    String cleanTaskId = task.getTaskId().substring(task.getTaskId().lastIndexOf("#") + 1);
                    String[] taskLine = {
                            " Tarefa",
                            "","",
                            cleanTaskId,
                            task.getTitle(),
                            task.getDescription(),
                            String.valueOf(task.isCompleted())
                    };
                    csvWriter.writeNext(taskLine);

                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar o arquivo CSV", e);
        }
        return stringWriter.toString();
    }
}
