package com.mediaalterations.mainservice.dto;

import com.mediaalterations.mainservice.entity.ProcessStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProcessDto(

        UUID id,

        String storageIdInput,
        String storageIdOutput,
        String storageInputPath,
        String finalFileSize,
        String command,

        ProcessStatus status,

        String userId,

        LocalDateTime created_at
) {
}
