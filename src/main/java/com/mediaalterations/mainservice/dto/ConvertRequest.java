package com.mediaalterations.mainservice.dto;

public sealed interface ConvertRequest permits AudioConvertRequest, VideoConvertRequest, GifConvertRequest {
    String storageId();

    String fileName();

    String duration();

    String toMediaType();

}
