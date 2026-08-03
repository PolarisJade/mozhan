package com.god.mz.controller.user;

import com.god.mz.common.enums.LikeTypeEnum;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.ILikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/like")
public class LikeController {

    private final ILikeService likeService;

    @PostMapping("/{type}/{id}")
    public Result<Void> like(@PathVariable LikeTypeEnum type, @PathVariable Long id) {
        likeService.like(type, id);
        return Result.success();
    }

    @DeleteMapping("/{type}/{id}")
    public Result<Void> cancelLike(@PathVariable LikeTypeEnum type, @PathVariable Long id) {
        likeService.cancelLike(type, id);
        return Result.success();
    }
}
