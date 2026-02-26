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
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessServiceImpl implements ProcessService {

        private final ProcessRepository processRepository;
        private final RabbitMQProducer processProducer;
        private final StorageClient storageClient;
        private final ExecutorService virtualExecutor;

        @Override
        public void transcodeVideo(AudioConvertRequest request, String userId) {
        }

        // ===================== EXTRACT AUDIO =====================

        private TranscodeResponse createProcess(
                        ConvertRequest request,
                        Runnable validator,
                        BiFunction<String, String, String> commandBuilder,
                        String userId) throws InterruptedException, ExecutionException {
                validator.run();
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

                String ffmpegCmd = commandBuilder.apply(inputPath, output.path());

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
                                "Processing started successfully", mapToResponseDto(process),
                                queueNo);

        }

        @Override
        @Transactional
        public TranscodeResponse extractAndConvertAudio(
                        AudioConvertRequest request,
                        String userId) {

                log.info("Extract audio request received. userId={}, storageId={}",
                                userId, request.storageId());

                try {
                        return createProcess(
                                        request,
                                        () -> validateRequest(request),
                                        (inputPath, outputPath) -> buildFfmpegCommand(request, inputPath, outputPath),
                                        userId);
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

        @Override
        public TranscodeResponse convertVideoToAnotherFormat(VideoConvertRequest request, String userId) {
                log.info("Extract audio request received. userId={}, storageId={}",
                                userId, request.storageId());

                try {
                        return createProcess(
                                        request,
                                        () -> validateRequest(request),
                                        (inputPath, outputPath) -> buildFfmpegCommand(request, inputPath, outputPath),
                                        userId);
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

        @Override
        public TranscodeResponse convertVideoToGif(GifConvertRequest request, String userId) {
                log.info("Convert video to GIF request received. userId={}, storageId={}",
                                userId, request.storageId());

                try {
                        return createProcess(
                                        request,
                                        () -> validateRequest(request),
                                        (inputPath, outputPath) -> buildFfmpegCommand(request, inputPath, outputPath),
                                        userId);
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

        private void validateRequest(AudioConvertRequest request) {
                if (!isValidMediaType(request.toMediaType())) {
                        log.warn("Invalid media type for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported media type", null);
                }
                if ((MediaType.flac.name().equals(request.toMediaType())
                                || MediaType.wav.name().equals(request.toMediaType()))
                                && !isValidChannelType(request.channelType())) {
                        log.warn("Invalid channel type for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported channel type", null);
                }
                if ((MediaType.flac.name().equals(request.toMediaType())
                                || MediaType.wav.name().equals(request.toMediaType()))
                                && !isValidBitrate(request.bitrate())) {
                        log.warn("Invalid bitrate for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported Bitrate", null);
                }

        }

        private void validateRequest(VideoConvertRequest request) {
                if (!isValidMediaType(request.toMediaType())) {
                        log.warn("Invalid media type for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported media type", null);
                }
                if (!isValidVideoCodec(request.videoCodec())) {
                        log.warn("Invalid video codec for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported video codec", null);
                }
                if (!isValidAudioCodec(request.audioCodec())) {
                        log.warn("Invalid audio codec for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported audio codec", null);
                }
                if (!isValidEncoderPreset(request.encoderPreset())) {
                        log.warn("Invalid encoder preset for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported encoder preset", null);
                }
                if (!isValidResolution(request.resolution())) {
                        log.warn("Invalid resolution for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported resolution", null);
                }
                if (request.crf() < 0 || request.crf() > 51) {
                        log.warn("Invalid CRF value for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported CRF value", null);
                }
                if (request.frameRate() < 0 || request.frameRate() > 240)
                        throw new ProcessCreationException("Invalid frame rate", null);

                VideoCodecType videoCodec = VideoCodecType.valueOf(request.videoCodec());
                AudioCodecType audioCodec = AudioCodecType.valueOf(request.audioCodec());

                if (videoCodec == VideoCodecType.source && request.frameRate() > 0)
                        throw new ProcessCreationException("Cannot change frame rate when video codec is source", null);

                if (videoCodec == VideoCodecType.source && !ResolutionType.source.name().equals(request.resolution()))
                        throw new ProcessCreationException("Cannot change resolution when video codec is source", null);

                if (request.toMediaType().equalsIgnoreCase(MediaType.mp4.name())) {
                        if (videoCodec == VideoCodecType.vp9 || videoCodec == VideoCodecType.av1)
                                throw new ProcessCreationException("VP9/AV1 not recommended in MP4 container", null);

                        if (audioCodec == AudioCodecType.flac || audioCodec == AudioCodecType.dts)
                                throw new ProcessCreationException("FLAC/DTS not supported in MP4", null);
                }

                if (request.toMediaType().equalsIgnoreCase(MediaType.webm.name())) {
                        if (!(videoCodec == VideoCodecType.vp9 || videoCodec == VideoCodecType.av1
                                        || videoCodec == VideoCodecType.source))
                                throw new ProcessCreationException("WebM supports VP9 or AV1 video only", null);

                        if (!(audioCodec == AudioCodecType.opus || audioCodec == AudioCodecType.source))
                                throw new ProcessCreationException("WebM supports Opus audio only", null);
                }
        }

        private void validateRequest(GifConvertRequest request) {
                if (!isValidResolution(request.resolution())) {
                        log.warn("Invalid resolution for fileName={}", request.fileName());
                        throw new ProcessCreationException("Unsupported resolution", null);
                }
                if (request.fps() < 0 || request.fps() > 240)
                        throw new ProcessCreationException("Invalid frame rate", null);
                if (request.startTimeSeconds() < 0)
                        throw new ProcessCreationException("Invalid start time", null);
                if (request.durationSeconds() <= 0)
                        throw new ProcessCreationException("Invalid duration", null);
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

        private boolean isValidVideoCodec(String codec) {
                try {
                        VideoCodecType.valueOf(codec);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        private boolean isValidAudioCodec(String codec) {
                try {
                        AudioCodecType.valueOf(codec);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        private boolean isValidEncoderPreset(String preset) {
                try {
                        EncodingPresetType.valueOf(preset);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        private boolean isValidResolution(String resolution) {
                try {
                        ResolutionType.valueOf(resolution);
                        return true;
                } catch (Exception e) {
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
                        AudioConvertRequest request,
                        String inputPath,
                        String outputPath) {

                boolean isVideo = false;
                if (request.fileName() != null && request.fileName().contains(".")) {
                        String extension = request.fileName().substring(request.fileName().lastIndexOf(".") + 1);
                        isVideo = extension.matches("(?i)mp4|mkv|avi|mov|flv|webm|wmv|m4v|3gp");
                }
                StringBuilder cmd = new StringBuilder();

                cmd.append("-y -i ").append(inputPath)
                                .append(" -progress pipe:1 ");

                if (isVideo)
                        cmd.append("-vn ");

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

        private String buildFfmpegCommand(
                        VideoConvertRequest request,
                        String inputPath,
                        String outputPath) {

                StringBuilder cmd = new StringBuilder();

                cmd.append("-y -i ").append(inputPath).append(" -progress pipe:1 ");

                boolean videoCopy = VideoCodecType.source.name().equalsIgnoreCase(request.videoCodec());
                boolean audioCopy = AudioCodecType.source.name().equalsIgnoreCase(request.audioCodec());

                // ========================
                // STREAM MAPPING
                // ========================
                cmd.append("-map 0:v:0 -map 0:a? ");

                // ========================
                // VIDEO SECTION
                // ========================

                if (videoCopy) {
                        cmd.append("-c:v copy ");
                } else {

                        // ---- Select Video Codec ----
                        String videoCodec = switch (VideoCodecType.valueOf(request.videoCodec())) {
                                case h264 -> "libx264";
                                case h265 -> "libx265";
                                case vp9 -> "libvpx-vp9";
                                case av1 -> "libaom-av1";
                                default -> throw new ProcessCreationException("Unsupported video codec", null);
                        };

                        cmd.append("-c:v ").append(videoCodec).append(" ");

                        // ---- Preset (only for x264/x265) ----
                        if (videoCodec.equals("libx264") || videoCodec.equals("libx265")) {
                                if (request.encoderPreset() != null &&
                                                !request.encoderPreset().equalsIgnoreCase("source")) {

                                        cmd.append("-preset ")
                                                        .append(request.encoderPreset().toLowerCase())
                                                        .append(" ");
                                }

                                // CRF only for x264/x265
                                if (request.crf() > 0) {
                                        cmd.append("-crf ").append(request.crf()).append(" ");
                                }
                        }

                        // ---- Frame Rate ----
                        if (request.frameRate() > 0) {
                                cmd.append("-r ").append(request.frameRate()).append(" ");
                        }

                        // ---- Resolution ----
                        if (request.resolution() != null &&
                                        !request.resolution().equalsIgnoreCase("source")) {

                                String scale = switch (request.resolution().toLowerCase()) {
                                        case "p144" -> "-2:144";
                                        case "p240" -> "-2:240";
                                        case "p360" -> "-2:360";
                                        case "p480" -> "-2:480";
                                        case "p720" -> "-2:720";
                                        case "p1080" -> "-2:1080";
                                        case "p1440" -> "-2:1440";
                                        case "p2160" -> "-2:2160";
                                        default -> throw new IllegalArgumentException("Unsupported resolution");
                                };

                                cmd.append("-vf scale=").append(scale).append(" ");
                        }

                        // Pixel format for compatibility
                        cmd.append("-pix_fmt yuv420p ");
                }

                // ========================
                // AUDIO SECTION
                // ========================

                if (audioCopy) {
                        cmd.append("-c:a copy ");
                } else {

                        String audioCodec = switch (request.audioCodec().toLowerCase()) {
                                case "aac" -> "aac";
                                case "ac3" -> "ac3";
                                case "flac" -> "flac";
                                case "dts" -> "dca";
                                case "opus" -> "libopus";
                                default -> throw new IllegalArgumentException("Unsupported audio codec");
                        };

                        cmd.append("-c:a ").append(audioCodec).append(" ");

                        // Bitrate only for lossy codecs
                        if (!audioCodec.equals("flac")) {
                                cmd.append("-b:a 192k ");
                        }
                }

                // ========================
                // FASTSTART (For MP4 streaming)
                // ========================
                if (request.toMediaType().equalsIgnoreCase("mp4")) {
                        cmd.append("-movflags +faststart ");
                }

                cmd.append(outputPath);

                String finalCommand = cmd.toString().trim();

                log.debug("Generated FFmpeg command: {}", finalCommand);

                return finalCommand;
        }

        private String buildFfmpegCommand(
                        GifConvertRequest request,
                        String inputPath,
                        String outputPath) {

                StringBuilder cmd = new StringBuilder();

                cmd.append("-y ");

                if (request.startTimeSeconds() > 0) {
                        cmd.append("-ss ").append(request.startTimeSeconds()).append(" ");
                }

                cmd.append("-i ").append(inputPath).append(" ");

                if (request.durationSeconds() > 0) {
                        cmd.append("-t ").append(request.durationSeconds()).append(" ");
                }

                cmd.append("-progress pipe:1 ");

                int fps = request.fps() > 0 ? request.fps() : 10;

                String scale = "-2:480"; // default

                if (request.resolution() != null &&
                                !request.resolution().equalsIgnoreCase("source")) {

                        scale = switch (request.resolution().toLowerCase()) {
                                case "p144" -> "-2:144";
                                case "p240" -> "-2:240";
                                case "p360" -> "-2:360";
                                case "p480" -> "-2:480";
                                case "p720" -> "-2:720";
                                case "p1080" -> "-2:1080";
                                default -> throw new IllegalArgumentException("Unsupported resolution");
                        };
                }

                String filter = "fps=" + fps +
                                ",scale=" + scale +
                                ":flags=lanczos,split[s0][s1];" +
                                "[s0]palettegen[p];" +
                                "[s1][p]paletteuse";

                cmd.append("-filter_complex ").append(filter).append(" ");

                cmd.append("-loop 0 ");

                cmd.append(outputPath);

                String finalCommand = cmd.toString().trim();

                log.debug("Generated FFmpeg GIF command: {}", finalCommand);

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
