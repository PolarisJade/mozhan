package com.god.mz.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.config.OssConfig;
import com.god.mz.exception.BizException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class OssUtil {

    @Resource
    private OSS ossClient;

    @Resource
    private OssConfig ossConfig;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    public String uploadImage(MultipartFile file) {
        validateFile(file, MAX_IMAGE_SIZE, new String[]{"jpg", "jpeg", "png", "gif"});
        return upload(file, "images");
    }

    public String uploadAvatar(MultipartFile file) {
        validateFile(file, MAX_AVATAR_SIZE, new String[]{"jpg", "jpeg", "png"});
        return upload(file, "avatars");
    }

    private String upload(MultipartFile file, String directory) {
        try {
            String originalFilename = file.getOriginalFilename();
            assert originalFilename != null;
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectName = directory + "/" + datePath + "/" + fileName;

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    objectName,
                    file.getInputStream()
            );

            ossClient.putObject(putObjectRequest);

            return ossConfig.getDomain() + "/" + objectName;

        } catch (IOException e) {
            throw new BizException(BizCodeEnum.FILE_UPLOAD_FAILED);
        }
    }

    private void validateFile(MultipartFile file, long maxSize, String[] allowedExtensions) {
        if (file.isEmpty()) {
            throw new BizException(BizCodeEnum.FILE_UPLOAD_FAILED);
        }

        if (file.getSize() > maxSize) {
            throw new BizException(BizCodeEnum.FILE_SIZE_EXCEEDED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BizException(BizCodeEnum.FILE_FORMAT_NOT_SUPPORTED);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        boolean isValid = false;
        for (String allowed : allowedExtensions) {
            if (allowed.equals(extension)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new BizException(BizCodeEnum.FILE_FORMAT_NOT_SUPPORTED);
        }
    }
}
