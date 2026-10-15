package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Integer> {

    List<ApplicationDocument> findByApplicationID(Integer applicationID);

    // Used when a student cancels an application: the physical file is only
    // deleted from disk if no other document row still points at it.
    long countByFilePath(String filePath);

}