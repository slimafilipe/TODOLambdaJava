package dev.filipe.TODOLambdaJava.controller.queue;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.dto.ReportRequestDTO;
import dev.filipe.TODOLambdaJava.util.ApiResponseBuilder;
import dev.filipe.TODOLambdaJava.util.AuthUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class StartReportHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Gson gson = new Gson();
    private final String queueReportUrl;
    private final SqsClient sqsClient;

    public StartReportHandler(){
        this.sqsClient = DependencyFactory.getSqsClient();
        String queueUrlEnv = System.getenv("QUEUE_URL");
        if (queueUrlEnv == null || queueUrlEnv.isEmpty()){
            throw new IllegalStateException("Variável de ambiente QUEUE_URL não está definida.");
        }
        this.queueReportUrl = queueUrlEnv;
    }
    public StartReportHandler(SqsClient sqsClient, String queueReportUrl) {

        this.sqsClient = sqsClient;
        this.queueReportUrl = queueReportUrl;

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


            ReportRequestDTO requestPayload = new ReportRequestDTO(userIdOpt.get(), userEmailOpt.get());

            String requestBody = gson.toJson(requestPayload);
            sqsClient.sendMessage(SendMessageRequest.builder()
                            .queueUrl(queueReportUrl)
                            .messageBody(requestBody)
                    .build());
            return ApiResponseBuilder.createSuccessResponse(202, "Sua solicitação foi recebida. Estamos processando-a. Em instantes você receberá em seu e-mail o arquivo .csv");
        } catch (Exception e) {
            logger.log("Erro no servidor interno: " +  e.getMessage());
            logger.log("Stace track: " + java.util.Arrays.toString(e.getStackTrace()));
            return ApiResponseBuilder.createErrorResponse(500, "Erro no servidor interno");
        }
    }
}
