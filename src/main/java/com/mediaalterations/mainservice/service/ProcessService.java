package com.mediaalterations.mainservice.service;

import com.mediaalterations.mainservice.dto.ProcessResponseDto;
import com.mediaalterations.mainservice.dto.AudioConvertRequest;
import com.mediaalterations.mainservice.dto.GifConvertRequest;
import com.mediaalterations.mainservice.dto.TranscodeResponse;
import com.mediaalterations.mainservice.dto.VideoConvertRequest;
import com.mediaalterations.mainservice.entity.ProcessStatus;

import java.util.List;

import org.jspecify.annotations.Nullable;

public interface ProcessService {
    void transcodeVideo(AudioConvertRequest request, String userId);

    TranscodeResponse extractAndConvertAudio(AudioConvertRequest request, String userId) throws Exception;

    void deleteProcessAndStorage(List<String> processId, String userId);

    List<ProcessResponseDto> getAllProcessOfUser(String userId);

    String updateStatusForProcess(ProcessStatus status, String fileSize, String fileDuration, String processId);

    TranscodeResponse convertVideoToAnotherFormat(VideoConvertRequest request, String userId);

    TranscodeResponse convertVideoToGif(GifConvertRequest request, String userId);
}
