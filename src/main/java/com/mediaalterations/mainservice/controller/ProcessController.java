package com.mediaalterations.mainservice.controller;

import com.mediaalterations.mainservice.dto.ProcessDto;
import com.mediaalterations.mainservice.dto.ProcessResponseDto;
import com.mediaalterations.mainservice.dto.TranscodeRequest;
import com.mediaalterations.mainservice.dto.TranscodeResponse;
import com.mediaalterations.mainservice.entity.ProcessStatus;
import com.mediaalterations.mainservice.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    @PostMapping("/video/transcode")
    public ResponseEntity<String> transcodeVideo(
            @RequestBody TranscodeRequest request,
            @RequestHeader("user_id") String userId) {
        processService.transcodeVideo(request, userId);
        return ResponseEntity.ok("");
    }

    @PostMapping("/video/toAudio")
    public ResponseEntity<TranscodeResponse> extractAudioFromVideo(
            @RequestBody TranscodeRequest request,
            @RequestHeader("user_id") String userId) throws Exception {
        return ResponseEntity.ok(processService.extractAudioFromVideo(request, userId));
    }

    @PutMapping("/updateStatus/{status}/{fileSize}/{fileDuration}/{id}")
    ResponseEntity<String> updateStatusForProcess(
            @PathVariable("status") ProcessStatus status,
            @PathVariable("fileSize") String fileSize,
            @PathVariable("fileDuration") String fileDuration,
            @PathVariable("id") String processId) {
        processService.updateStatusForProcess(status, fileSize, fileDuration, processId);
        return ResponseEntity.ok("");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteProcessAndStorage(
            @RequestBody List<String> processIds,
            @RequestHeader("user_id") String userId) {
        processService.deleteProcessAndStorage(processIds, userId);
        return ResponseEntity.ok("");
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<ProcessResponseDto>> getAllProcessOfUser(
            @RequestHeader("user_id") String userId) throws Exception {
        return ResponseEntity.ok(processService.getAllProcessOfUser(userId));
    }
}
