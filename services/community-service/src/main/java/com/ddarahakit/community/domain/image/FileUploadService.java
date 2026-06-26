package com.ddarahakit.community.domain.image;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    ResponseEntity<byte[]> display(String fileName);

    String uploadCommunityImage(MultipartFile file);
}
