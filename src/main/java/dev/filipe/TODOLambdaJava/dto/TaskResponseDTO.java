package dev.filipe.TODOLambdaJava.dto;

public record TaskResponseDTO(
        String id,
        String title,
        String description,
        boolean completed,
        String createdAt
) {}
