package com.god.mz.controller.user;


import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.CursorQuery;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.user.UserFollowItemVO;
import com.god.mz.service.IUserFollowService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户关注记录表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/user/follow")
public class UserFollowController {
    @Resource
    private IUserFollowService userFollowService;


    @PostMapping("/{userId}")
    public Result<Object> followUser(@PathVariable Long userId){
        userFollowService.followUser(userId);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    public Result<Object> cancelFollowUser(@PathVariable Long userId){
        userFollowService.cancelFollowUser(userId);
        return Result.success();
    }

    @GetMapping("/following")
    public Result<CursorPageVO<UserFollowItemVO>> queryFollowingList(CursorQuery query){
        CursorPageVO<UserFollowItemVO> pageVO = userFollowService.queryFollowingList(query);
        return Result.success(pageVO);
    }

    @GetMapping("/follower")
    public Result<CursorPageVO<UserFollowItemVO>> queryFollowerList(CursorQuery query){
        CursorPageVO<UserFollowItemVO> pageVO = userFollowService.queryFollowerList(query);
        return Result.success(pageVO);
    }
}
