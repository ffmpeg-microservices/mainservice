package com.mediaalterations.mainservice.service;

import com.mediaalterations.mainservice.dto.*;
import com.mediaalterations.mainservice.entity.Process;
import com.mediaalterations.mainservice.entity.ProcessStatus;
import com.mediaalterations.mainservice.exceptions.ExternalServiceException;
import com.mediaalterations.mainservice.exceptions.ProcessCreationException;
import com.mediaalterations.mainservice.exceptions.ProcessNotFoundException;
import com.mediaalterations.mainservice.feignClients.StorageClient;
import com.mediaalterations.mainservice.messaging.RabbitMQProducer;
import com.mediaalterations.mainservice.repository.ProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessServiceImpl implements ProcessService {

        private final ProcessRepository processRepository;
        private final RabbitMQProducer processProducer;
        private final StorageClient storageClient;
        private final ExecutorService virtualExecutor;

        @Override
        public void transcodeVideo(TranscodeRequest request, String userId) {
        }

        // ===================== EXTRACT AUDIO =====================

        @Override
        @Transactional
        public TranscodeResponse extractAudioFromVideo(
                        TranscodeRequest request,
                        String userId) {

                log.info("Extract audio request received. userId={}, storageId={}",
                                userId, request.storageId());

                try {
                        validateRequest(request);
                        Future<ResponseEntity<String>> inputFuture = virtualExecutor
                                        .submit(() -> storageClient.getPathFromStorageId(
                                                        request.storageId(), userId));

                        Future<ResponseEntity<OutputPathResponse>> outputFuture = virtualExecutor
                                        .submit(() -> storageClient.generateOutputPath(
                                                        request.fileName(),
                                                        request.toMediaType(),
                                                        userId));

                        ResponseEntity<String> inputRes = inputFuture.get();
                        ResponseEntity<OutputPathResponse> outputRes = outputFuture.get();

                        if (inputRes.getStatusCode().isError()
                                        || outputRes.getStatusCode().isError()) {

                                log.error("Storage service error during process creation.");
                                throw new ExternalServiceException("Storage service failure");
                        }

                        String inputPath = inputRes.getBody();
                        OutputPathResponse output = outputRes.getBody();

                        String ffmpegCmd = buildFfmpegCommand(request, inputPath, output.path());

                        Process process = new Process(
                                        request.storageId(),
                                        output.storageId(),
                                        ffmpegCmd,
                                        ProcessStatus.WAITING,
                                        userId,
                                        request.duration(),
                                        extractFileName(output.path()),
                                        "0 KB",
                                        false);

                        process = processRepository.save(process);

                        log.info("Process created successfully. processId={}, userId={}",
                                        process.getId(), userId);

                        processProducer.publishProcessCreated(
                                        mapToDto(process, inputPath, output.path(), extractFileName(output.path())));

                        int queueNo = processRepository.countByStatusAndCreatedAtBefore(
                                        ProcessStatus.WAITING,
                                        process.getCreatedAt());

                        log.info("Queue position for processId={} is {}",
                                        process.getId(), queueNo);

                        return new TranscodeResponse(
                                        "Processing started successfully",
                                        queueNo);

                } catch (ProcessCreationException e) {

                        log.error("{} userId={}, storageId={}", e.getMessage(), userId, request.storageId());

                        throw new ProcessCreationException(
                                        e.getMessage(), e);
                } catch (Exception e) {

                        log.error("Failed to create process. userId={}, storageId={}",
                                        userId, request.storageId(), e);

                        throw new ProcessCreationException(
                                        "Failed to create media process", e);
                }
        }

        // ===================== VALIDATION =====================

        private void validateRequest(TranscodeRequest request) {
                if (!isValidMediaType(request.toMediaType())) {
                        log.warn("Invalid media type for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported media type", null);
                }
                if (!isValidChannelType(request.channelType())) {
                        log.warn("Invalid channel type for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported channel type", null);
                }
                if (!isValidBitrate(request.bitrate())) {
                        log.warn("Invalid bitrate for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported Bitrate", null);
                }

        }

        private boolean isValidMediaType(String mediaType) {
                try {
                        MediaType.valueOf(mediaType);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        private boolean isValidChannelType(String channelType) {
                try {
                        ChannelType.valueOf(channelType);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        private boolean isValidBitrate(int bitrate) {
                switch (bitrate) {
                        case 64:
                        case 96:
                        case 128:
                        case 192:
                        case 256:
                        case 320:
                                return true;
                        default:
                                return false;
                }
        }

        // ===================== UPDATE STATUS =====================

        @Override
        @Transactional
        public String updateStatusForProcess(
                        ProcessStatus status,
                        String fileSize,
                        String fileDuration,
                        String processId) {

                log.debug("Updating process. processId={}, status={}, fileSize={}, fileDuration={}",
                                processId, status, fileSize, fileDuration);

                Process process = processRepository.findById(
                                UUID.fromString(processId))
                                .orElseThrow(() -> {
                                        log.warn("Process not found. processId={}", processId);
                                        return new ProcessNotFoundException("No process found");
                                });

                process.setStatus(status);
                process.setFinalFileSize(fileSize);
                process.setDuration(fileDuration);

                log.info("Process status updated. processId={}, newStatus={}",
                                processId, status);

                return "Updated Status!";
        }

        // ===================== DELETE =====================

        @Override
        @Transactional
        public void deleteProcessAndStorage(
                        List<String> processIds,
                        String userId) {

                log.info("Delete process request. userId={}, totalProcesses={}",
                                userId, processIds.size());

                List<UUID> uuids = processIds.stream()
                                .map(UUID::fromString)
                                .toList();

                List<String> storageIds = processRepository.getStorageIds(uuids, userId);

                if (storageIds.isEmpty()) {
                        log.warn("No valid processes found for deletion. userId={}", userId);
                        throw new ProcessNotFoundException("No valid files found");
                }

                storageClient.deleteStorage(storageIds, userId);

                processRepository.deleteByIdsAndUserId(uuids, userId);

                log.info("Processes deleted successfully. userId={}", userId);
        }

        // ===================== FETCH ALL =====================

        @Override
        public List<ProcessResponseDto> getAllProcessOfUser(String userId) {

                log.debug("Fetching all processes for userId={}", userId);

                List<Process> processes = processRepository.getAllByUserId(userId);

                return processes.stream()
                                .map(ProcessServiceImpl::mapToResponseDto)
                                .toList();
        }

        // ===================== HELPERS =====================

        private String buildFfmpegCommand(
                        TranscodeRequest request,
                        String inputPath,
                        String outputPath) {

                StringBuilder cmd = new StringBuilder();

                cmd.append("-y -i ").append(inputPath)
                                .append(" -progress pipe:1 -vn ");

                String codec = "libmp3lame";
                String bitrate = request.bitrate() + "k";

                MediaType mediaType = MediaType.valueOf(request.toMediaType());

                switch (mediaType) {
                        case mp3 -> codec = "libmp3lame";
                        case aac, m4a -> codec = "aac";
                        case wav -> {
                                codec = "pcm_s16le";
                                bitrate = null;
                        }
                        case flac -> {
                                codec = "flac";
                                bitrate = null;
                        }
                        case ogg -> codec = "libvorbis";
                }

                cmd.append("-c:a ").append(codec).append(" ");

                // Only add channel option when needed
                if (!mediaType.equals(MediaType.wav) && !mediaType.equals(MediaType.flac)) {
                        cmd.append("-ac ")
                                        .append(request.channelType().equals("STEREO") ? "2 " : "1 ");
                }

                cmd.append("-ar ").append(request.sampleRate()).append(" ");

                if (bitrate != null) {
                        cmd.append("-b:a ").append(bitrate).append(" ");
                }

                cmd.append(outputPath);

                String finalCommand = cmd.toString().trim();

                log.debug("Generated FFmpeg command: {}", finalCommand);

                return finalCommand;
        }

        private String extractFileName(String path) {
                return Path.of(path).getFileName().toString();
        }

        private static ProcessResponseDto mapToResponseDto(Process p) {
                return new ProcessResponseDto(
                                p.getFileName(),
                                p.getDuration(),
                                p.getFinalFileSize(),
                                p.isVideo(),
                                p.getId().toString(),
                                p.getStorageIdOutput(),
                                p.getStatus(),
                                p.getCreatedAt());
        }

        private ProcessDto mapToDto(Process process, String inputPath, String outputPath, String fileName) {
                return new ProcessDto(
                                process.getId(),
                                process.getStorageIdInput(),
                                process.getStorageIdOutput(),
                                inputPath,
                                outputPath,
                                fileName,
                                process.getFinalFileSize(),
                                process.getCommand(),
                                process.getStatus(),
                                process.getUserId(),
                                process.getCreatedAt());
        }
}
