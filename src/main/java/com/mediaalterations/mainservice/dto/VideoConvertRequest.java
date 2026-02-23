package com.mediaalterations.mainservice.dto;

public record VideoConvertRequest(
                String storageId,
                String fileName,
                String duration,

                String toMediaType,
                String videoCodec,
                String audioCodec,
                String encoderPreset,
                int crf,
                int frameRate,
                String resolution

) {

}
