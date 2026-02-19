package com.mediaalterations.mainservice.repository;

import com.mediaalterations.mainservice.entity.Process;
import com.mediaalterations.mainservice.entity.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessRepository extends JpaRepository<Process, UUID> {

    @Query(
            "select p.storageIdOutput from Process p where p.id in :ids and p.userId=:userId"
    )
    List<String> getStorageIds(@Param("ids") List<UUID> ids, @Param("userId") String userId);

    @Modifying
    @Query("""
       DELETE FROM Process p
       WHERE p.id IN :ids
       AND p.userId = :userId
       """)
    void deleteByIdsAndUserId(
            @Param("ids") List<UUID> ids,
            @Param("userId") String userId
    );

    int countByStatusAndCreatedAtBefore(ProcessStatus status, LocalDateTime createdAt);

    List<Process> getAllByUserId(String userId);

}
