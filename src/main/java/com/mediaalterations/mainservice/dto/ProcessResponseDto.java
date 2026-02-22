package com.mediaalterations.mainservice.dto;

import com.mediaalterations.mainservice.entity.ProcessStatus;

import java.time.LocalDateTime;

public record ProcessResponseDto(
                String fileName,
                String duration,
                String finalFileSize,
                boolean isVideo,
                String processId,
                String storageIdOutput,
                ProcessStatus status,
                LocalDateTime createdAt) {
}
