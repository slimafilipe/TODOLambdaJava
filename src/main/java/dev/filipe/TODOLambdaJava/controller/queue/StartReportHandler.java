package dev.filipe.TODOLambdaJava.controller.queue;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class StartReportHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Gson gson = new Gson();
    private final String QUEUE_REPORT = System.getenv("QUEUE_URL");
    private final SqsClient sqsClient;

    public StartReportHandler(){this.sqsClient = DependencyFactory.getSqsClient();
    }
    public StartReportHandler(SqsClient sqsClient) {

        this.sqsClient = sqsClient;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var logger = context.getLogger();
        logger.log("Recebida requisição para enviar mensagem a fila");

        try {

            Optional<String> userIdOpt = AuthUtils.getUserId(input);
            if (userIdOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado!");
            }
            Optional<String> userEmailOpt= AuthUtils.getUserEmail(input);
            if (userEmailOpt.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(404, "Email do usuário não encontrado!");
            }

            Map<String, String> messagePayload = new HashMap<>();
            userIdOpt.ifPresent(id -> messagePayload.put("userId", id));
            userEmailOpt.ifPresent( email -> messagePayload.put("email", email));

            String requestBody = gson.toJson(messagePayload);
            sqsClient.sendMessage(SendMessageRequest.builder()
                            .queueUrl(QUEUE_REPORT)
                            .messageBody(requestBody)
                            .delaySeconds(10)
                    .build());
            return ApiResponseBuilder.createSuccessResponse(202, "Sua solicitação foi recebida. Estamos processando-a. Em instantes você receberá em seu e-mail o arquivo .csv");
        } catch (Exception e) {
            return ApiResponseBuilder.createErrorResponse(500, "Erro no servidor interno");
        }
    }
}
