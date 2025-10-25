package dev.filipe.TODOLambdaJava.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public final class AuthUtils {

    private static final Logger log = LoggerFactory.getLogger(AuthUtils.class);

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
    public static Optional<String> getUserEmail(APIGatewayProxyRequestEvent input) {
        try {
            var requestContext = input.getRequestContext();
            var authorizer = requestContext.getAuthorizer();

            Object claimsObject = authorizer.get("claims");

            if (claimsObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> claims = (Map<String, String>) authorizer.get("claims");
                String userEmail = claims.get("email");
                if (userEmail != null && !userEmail.isEmpty()) {
                    return Optional.of(userEmail);
                }
            }
        }catch (Exception e){
            return Optional.empty();
        }
        return Optional.empty();
    }

}
