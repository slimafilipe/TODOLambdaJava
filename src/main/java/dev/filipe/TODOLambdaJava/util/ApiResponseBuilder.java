package dev.filipe.TODOLambdaJava.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.Map;

public class ApiResponseBuilder {
    private static final Gson gson = new Gson();

    public ApiResponseBuilder(){

    }

    public static APIGatewayProxyResponseEvent createSucessResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(gson.toJson(body))
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
    }


    public static APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String errorMessage) {
        Map<String, String> errorPayload = Collections.singletonMap("Erro: ", errorMessage);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(gson.toJson(errorPayload))
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
    }
}
