package com.mediaalterations.mainservice.dto;

public record TranscodeRequest(
                String storageId,
                String fileName,
                String duration,
                String toMediaType,
                int bitrate,
                String channelType,
                int sampleRate) {
}
