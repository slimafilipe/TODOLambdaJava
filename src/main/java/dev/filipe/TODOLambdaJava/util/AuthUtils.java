package dev.filipe.TODOLambdaJava.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Map;
import java.util.Optional;

public final class AuthUtils {

    private AuthUtils(){}

    public static Optional<String> getUserId(APIGatewayProxyRequestEvent input){
        try {
            var requestContext = input.getRequestContext();
            var authorizer = requestContext.getAuthorizer();

            Object claimsObject = authorizer.get("claims");

            if (claimsObject instanceof Map){
                @SuppressWarnings("unchecked")
                Map<String, String> claims = (Map<String, String>) authorizer.get("claims");
                String userId = claims.get("sub");
                if (userId != null && !userId.isEmpty()){
                    return Optional.of(userId);
                }
            }
        } catch (Exception e){
            return Optional.empty();
        }
        return Optional.empty();
    }

}
