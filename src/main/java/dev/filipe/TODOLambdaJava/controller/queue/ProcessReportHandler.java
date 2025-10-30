package dev.filipe.TODOLambdaJava.controller.queue;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;

import dev.filipe.TODOLambdaJava.config.DependencyFactory;
import dev.filipe.TODOLambdaJava.dto.ReportRequestDTO;
import dev.filipe.TODOLambdaJava.model.Task;
import dev.filipe.TODOLambdaJava.model.constants.Constants;
import dev.filipe.TODOLambdaJava.repository.TaskRepository;
import dev.filipe.TODOLambdaJava.util.CsvGenerator;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ProcessReportHandler implements RequestHandler<SQSEvent, Void> {
    private final Gson gson = new Gson();
    private final TaskRepository taskRepository;
    private final S3Client s3Client;
    private final SesClient sesClient;
    private final S3Presigner s3Presigner;

    private final String CSV_BUCKET_NAME = System.getenv("CSV_BUCKET_NAME");
    private final String SENDER_EMAIL = System.getenv("SENDER_EMAIL");

    public ProcessReportHandler(){
        this.taskRepository = DependencyFactory.getTaskRepository();
        this.s3Client = DependencyFactory.getS3Client();
        this.sesClient = DependencyFactory.getSesClient();
        this.s3Presigner = DependencyFactory.getS3Presigner();
    }


    public ProcessReportHandler(SesClient sesClient, S3Client s3Client, TaskRepository taskRepository, S3Presigner s3Presigner){
        this.sesClient = sesClient;
        this.s3Client = s3Client;
        this.taskRepository = taskRepository;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public Void handleRequest(SQSEvent input, Context context) {
        var logger = context.getLogger();
        for (SQSEvent.SQSMessage msg : input.getRecords()){
            try {
                String messageBody = msg.getBody();
                logger.log("Processando mensagem: " + messageBody);
                ReportRequestDTO reportRequest = gson.fromJson(messageBody, ReportRequestDTO.class);
                String userId = reportRequest.userId();
                String email = reportRequest.email();

                List<Task> allItems = taskRepository.getAllUserItems(userId);
                Map<String, List<Task>> tasksByListId = new HashMap<>();
                List<Task> taskList = new ArrayList<>();

                for (Task item : allItems){
                    String sk = item.getTaskId();
                    if (sk.contains("#" + Constants.TASK_PREFIX)){
                        String listSK = sk.substring(0, sk.indexOf("#" + Constants.TASK_PREFIX));
                        tasksByListId.computeIfAbsent(listSK, k -> new ArrayList<>()).add(item);
                    } else if (sk.startsWith(Constants.LIST_PREFIX)){
                        taskList.add(item);
                    }
                }
                logger.log("Encontradas " +taskList.size() + " listas e " + (allItems.size() - taskList.size()) + " tarefas.");

                String csvContent = CsvGenerator.generateHierarchicalCsv(taskList, tasksByListId);
                logger.log("CSV gerado com sucesso.");

                String s3Key = "reports/" + userId + "/" + System.currentTimeMillis() + "-report.csv";
                s3Client.putObject(
                        PutObjectRequest.builder().bucket(CSV_BUCKET_NAME).key(s3Key).build(),
                        RequestBody.fromString(csvContent)
                );
                logger.log("CSV salvo no S# em: " + s3Key);

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(20))
                        .getObjectRequest(req -> req.bucket(CSV_BUCKET_NAME).key(s3Key))
                        .build();
                PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
                String downloadUrl = presignedRequest.url().toString();
                logger.log("URL de download gerada: " + downloadUrl);

                String emailBody = "Olá\n\nSeu relatório de listas e tarefas está pronto.\n"
                        + "Você pode baixá-lo usando o link seguro abaixo(válido por 15 minutos):\n\n " + downloadUrl;
                sesClient.sendEmail(SendEmailRequest.builder()
                                .source(SENDER_EMAIL)
                                .destination(Destination.builder().toAddresses(email).build())
                                .message(Message.builder()
                                        .subject(Content.builder().data("Seu Relatório de Tarefas está pronto").build())
                                        .body(Body.builder().text(Content.builder().data(emailBody).build()).build())
                                        .build())
                        .build());
                logger.log("E-mail de notificação enviado para: " + email);
            } catch (Exception e) {
                logger.log("ERRO AO PROCESSAR MENSAGEM: " + e.getMessage());
                logger.log("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
                throw new RuntimeException("Falha ao processar mensagem SQS", e);
            }
        }
        return null;
    }
}
