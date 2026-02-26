package com.mediaalterations.mainservice.dto;

public record TranscodeResponse(
                String message,
                ProcessResponseDto processResponseDto,
                int queueNo) {
}
