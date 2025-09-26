package dev.filipe.TODOLambdaJava.Controller;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

public class UpdateTaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyRequestEvent> {
    @Override
    public APIGatewayProxyRequestEvent handleRequest(APIGatewayProxyRequestEvent input, com.amazonaws.services.lambda.runtime.Context context) {
        return null;
    }
}
