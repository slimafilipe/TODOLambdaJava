package dev.filipe.TODOLambdaJava.Controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import dev.filipe.TODOLambdaJava.Config.DependecyFactory;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;

public class DeleteTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final TaskRepository taskRepository;

    public DeleteTaskHandler() {
        this.taskRepository= DependecyFactory.getTaskRepositoryInstance();
    }
    public DeleteTaskHandler(TaskRepository taskRepository){
        this.taskRepository = taskRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        var logger = context.getLogger();
        logger.log("Requisição para deletar tarefa recebida com sucesso.");
        try{

        }

        return null;
    }
}
