package com.mediaalterations.mainservice.dto;

import java.util.List;

public record MergeConvertRequest(
        List<OrderedMedia> mediaFiles,
        String duration,
        String toMediaType,
        String videoCodec,
        String audioCodec,
        int resolutionHeight

) {

}
