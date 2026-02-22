package com.mediaalterations.mainservice.dto;

public record VideoConvertRequest(
        String storageId,
        String fileName,
        String duration,
        String toMediaType,
        int bitrate,
        int frameRate,
        String resolution) {

}
