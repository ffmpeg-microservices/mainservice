package com.mediaalterations.mainservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TranscodeRequest(
        String storageId,
        String fileName,
        String duration,
        String toMediaType,
        int bitrate,
        String channelType,
        int sampleRate) {
}
