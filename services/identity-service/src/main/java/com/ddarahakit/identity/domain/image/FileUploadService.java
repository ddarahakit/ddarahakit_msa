package com.ddarahakit.identity.domain.image;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    ResponseEntity<byte[]> display(String fileName);

    String uploadProfile(MultipartFile file);

    String uploadQnaImage(MultipartFile file);

    String uploadCommunityImage(MultipartFile file);
}
