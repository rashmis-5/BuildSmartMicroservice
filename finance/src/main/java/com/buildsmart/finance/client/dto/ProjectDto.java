package com.buildsmart.finance.client.dto;

public record ProjectDto(
        String projectId,
        String projectName,
        Double budget,
        String status
) {}
