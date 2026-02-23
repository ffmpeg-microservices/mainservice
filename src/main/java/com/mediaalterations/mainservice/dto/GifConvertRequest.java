package com.mediaalterations.mainservice.dto;

public record GifConvertRequest(
                String storageId,
                String fileName,
                String duration,
                String toMediaType,
                int startTimeSeconds,
                int durationSeconds,
                int fps,
                String resolution) implements ConvertRequest {

}
