package dev.filipe.TODOLambdaJava;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Handler implements RequestHandler<Object, String> {
    @Override
    public String handleRequest(Object o, Context context) {
        String message = "Hello do Lambda! É só o começo...";
        context.getLogger().log("Input: " + o);
        return message;
    }
}
