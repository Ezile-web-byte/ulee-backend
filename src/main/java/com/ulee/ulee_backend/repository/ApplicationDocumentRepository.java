package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Integer> {

    List<ApplicationDocument> findByApplicationID(Integer applicationID);


}