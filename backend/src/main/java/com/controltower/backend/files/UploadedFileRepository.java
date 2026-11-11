package com.controltower.backend.files;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
}
