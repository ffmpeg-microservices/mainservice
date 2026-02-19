package com.mediaalterations.mainservice.feignClients;

import com.mediaalterations.mainservice.dto.OutputPathResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name="storage-service", path = "/storage")
public interface StorageClient {

    @GetMapping("/getPath/{id}")
    public ResponseEntity<String> getPathFromStorageId(
            @PathVariable("id") String storageId,
            @RequestHeader("user_id") String userId);


    @GetMapping("/generateOutputPath/{filename}/{contentType}")
    public ResponseEntity<OutputPathResponse> generateOutputPath(
            @PathVariable("filename") String filename,
            @PathVariable("contentType") String contentType,
            @RequestHeader("user_id") String userId);

    @DeleteMapping("/delete")
    public ResponseEntity<List<String>> deleteStorage(
            @RequestBody List<String> storageIds,
            @RequestHeader("user_id") String userId);


}
