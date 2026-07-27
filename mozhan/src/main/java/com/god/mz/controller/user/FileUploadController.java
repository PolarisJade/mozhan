package com.god.mz.controller.user;

import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.file.UploadVO;
import com.god.mz.util.OssUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Resource
    private OssUtil ossUtil;

    @PostMapping("/image")
    public Result<UploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = ossUtil.uploadImage(file);
        UploadVO vo = new UploadVO(url, file.getOriginalFilename(), file.getSize());
        return Result.success(vo);
    }

    @PostMapping("/avatar")
    public Result<UploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = ossUtil.uploadAvatar(file);
        UploadVO vo = new UploadVO(url, file.getOriginalFilename(), file.getSize());
        return Result.success(vo);
    }
}
