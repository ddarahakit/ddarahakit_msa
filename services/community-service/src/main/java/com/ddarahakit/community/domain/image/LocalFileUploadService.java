package com.ddarahakit.community.domain.image;

import com.ddarahakit.community.common.exception.BaseException;
import com.ddarahakit.community.common.model.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.ddarahakit.community.common.Constants.COMMUNITY_IMAGE_UPLOAD_PATH;

@Slf4j
@Service
public class LocalFileUploadService implements FileUploadService {

    // 허용 이미지 확장자 화이트리스트 (소문자)
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    @Value("${project.upload.path}")
    private String defaultUploadPath;


    public String makeFolder(String uploadPath) {

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        String folderPath = date.replace("/", File.separator);

        File uploadPathFolder = new File(defaultUploadPath + uploadPath + File.separator + folderPath);
        if (!uploadPathFolder.exists()) {
            if (uploadPathFolder.mkdirs()) {
                return File.separator + folderPath;
            }
        }

        return File.separator + folderPath;
    }

    public String saveFile(MultipartFile file, String uploadPath) {
        if (file == null || file.isEmpty()) {
            throw BaseException.of(BaseResponseStatus.FILE_EMPTY);
        }

        // 확장자 화이트리스트 + Content-Type 이중 검증 (악성/비이미지 업로드 차단)
        String ext = extractAllowedExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw BaseException.of(BaseResponseStatus.FILE_INVALID_TYPE);
        }

        String folderPath = makeFolder(uploadPath);

        // 저장 파일명은 UUID + 확장자로만 구성한다. 원본 파일명을 경로에 포함하지 않아
        // 경로 조작(../)·특수문자 주입을 원천 차단한다.
        String saveFileName = uploadPath + folderPath + File.separator + UUID.randomUUID() + "." + ext;
        File saveFile = new File(defaultUploadPath + File.separator + saveFileName);

        try {
            file.transferTo(saveFile);
            return saveFileName.replace(File.separator, "/");
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", saveFileName, e);
            throw BaseException.of(BaseResponseStatus.FILE_UPLOAD_FAIL);
        }
    }

    // 원본 파일명에서 확장자만 추출해 화이트리스트로 검증한다. 미허용 시 예외.
    private String extractAllowedExtension(String originalName) {
        if (originalName == null) {
            throw BaseException.of(BaseResponseStatus.FILE_INVALID_TYPE);
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            throw BaseException.of(BaseResponseStatus.FILE_INVALID_TYPE);
        }
        String ext = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw BaseException.of(BaseResponseStatus.FILE_INVALID_TYPE);
        }
        return ext;
    }

    @Override
    public ResponseEntity<byte[]> display(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 백슬래시→슬래시 정규화 후 선행 슬래시 제거하여 업로드 루트 기준 상대경로로 해석
        String relativePath = fileName.replace("\\", "/").replaceFirst("^/+", "");

        // 경로 조작(../) 방어: 정규화된 대상 경로가 업로드 루트 하위인지 검증한다.
        Path root = Paths.get(defaultUploadPath).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            log.warn("업로드 루트 밖 파일 접근 차단: {}", fileName);
            return ResponseEntity.badRequest().build();
        }

        File file = target.toFile();
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Files.probeContentType() : 파일 Content 타입 인식(image/png ...), 미인식 시 null
            String contentType = Files.probeContentType(target);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            HttpHeaders header = new HttpHeaders();
            header.add("Content-Type", contentType);
            return new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), header, HttpStatus.OK);
        } catch (IOException e) {
            log.error("이미지 조회 실패: {}", relativePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public String uploadCommunityImage(MultipartFile file) {
        return saveFile(file, COMMUNITY_IMAGE_UPLOAD_PATH);
    }
}
