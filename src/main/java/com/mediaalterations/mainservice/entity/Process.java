package com.mediaalterations.mainservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Process {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String storageIdInput;
    private String storageIdOutput;

    private String command;

    @Enumerated(value = EnumType.STRING)
    private ProcessStatus status;

    private String userId;
    private String duration;
    private String fileName;
    private String finalFileSize;

    private boolean isVideo;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Process(String storageIdInput,String storageIdOutput, String command,
                   ProcessStatus status, String userId, String duration,
                   String fileName,String finalFileSize, boolean isVideo) {
        this.storageIdInput = storageIdInput;
        this.storageIdOutput = storageIdOutput;
        this.command = command;
        this.status = status;
        this.userId = userId;
        this.duration=duration;
        this.fileName=fileName;
        this.finalFileSize = finalFileSize;
        this.isVideo=isVideo;
    }
}
