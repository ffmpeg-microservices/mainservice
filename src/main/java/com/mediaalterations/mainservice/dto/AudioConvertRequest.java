package com.mediaalterations.mainservice.dto;

public record AudioConvertRequest(
                String storageId,
                String fileName,
                String duration,
                String toMediaType,
                int bitrate,
                String channelType,
                int sampleRate) implements ConvertRequest {
}
