package com.example.market.controller;

import com.example.market.dto.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin
public class UploadController {

    // 上传目录（本地磁盘路径）
    private static final String UPLOAD_DIR = "d:/APP/cursor/ku/cursor1/upload-images/";

    @PostMapping("/image")
    public Result<String> uploadImage(MultipartFile file) throws IOException {
        return uploadFile(file, new String[]{"image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"});
    }

    @PostMapping("/video")
    public Result<String> uploadVideo(MultipartFile file) throws IOException {
        return uploadFile(file, new String[]{"video/mp4", "video/webm", "video/ogg", "video/quicktime"});
    }

    private Result<String> uploadFile(MultipartFile file, String[] allowedContentTypes) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.fail("文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (contentType != null) {
            boolean allowed = false;
            for (String type : allowedContentTypes) {
                if (type.equalsIgnoreCase(contentType)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed && !isAllowedByExtension(originalFilename, allowedContentTypes)) {
                return Result.fail("不支持的文件类型");
            }
        }

        File dir = new File(UPLOAD_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return Result.fail("创建上传目录失败");
        }

        String ext = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String newName = datePrefix + "-" + UUID.randomUUID() + ext;

        File dest = new File(dir, newName);
        file.transferTo(dest);

        String url = "http://localhost:8081/upload/" + newName;
        return Result.ok(url);
    }

    private boolean isAllowedByExtension(String filename, String[] allowedContentTypes) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        for (String type : allowedContentTypes) {
            if (type.startsWith("image/") && (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")
                    || ext.equals(".gif") || ext.equals(".webp") || ext.equals(".bmp"))) {
                return true;
            }
            if (type.startsWith("video/") && (ext.equals(".mp4") || ext.equals(".webm") || ext.equals(".ogg") || ext.equals(".mov"))) {
                return true;
            }
        }
        return false;
    }
}

