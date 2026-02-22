package com.mediaalterations.mainservice.service;

import com.mediaalterations.mainservice.dto.ProcessResponseDto;
import com.mediaalterations.mainservice.dto.TranscodeRequest;
import com.mediaalterations.mainservice.dto.TranscodeResponse;
import com.mediaalterations.mainservice.entity.ProcessStatus;

import java.util.List;

public interface ProcessService {
    void transcodeVideo(TranscodeRequest request, String userId);

    TranscodeResponse extractAudioFromVideo(TranscodeRequest request, String userId) throws Exception;

    void deleteProcessAndStorage(List<String> processId, String userId);

    List<ProcessResponseDto> getAllProcessOfUser(String userId);

    String updateStatusForProcess(ProcessStatus status, String fileSize, String fileDuration, String processId);
}
