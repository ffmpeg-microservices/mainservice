package com.mediaalterations.mainservice.dto;

public record OrderedMedia(
                String storageId,
                String type // "video" or "audio"
) {
}