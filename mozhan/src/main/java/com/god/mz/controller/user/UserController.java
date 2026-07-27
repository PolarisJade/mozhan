package com.god.mz.controller.user;


import com.god.mz.domain.dto.UserLoginDTO;
import com.god.mz.domain.dto.UserPwdDTO;
import com.god.mz.domain.dto.UserUpdateDTO;
import com.god.mz.domain.po.User;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.user.UserLoginVO;
import com.god.mz.domain.vo.user.UserVO;
import com.god.mz.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private IUserService  userService;

    @PostMapping("register")
    public Result<Object> register(@RequestBody UserLoginDTO userLoginDTO){
        UserLoginVO userLoginVO = userService.register(userLoginDTO);
        return Result.success(userLoginVO);
    }

    @PostMapping("/login")
    public Result<Object> login(@RequestBody UserLoginDTO userLoginDTO){
        UserLoginVO userLoginVO = userService.login(userLoginDTO);
        return Result.success(userLoginVO);
    }

    @GetMapping("/info/{userId}")
    public Result<User> queryUserInfo(@PathVariable Long userId){
        User user = userService.queryUserInfo(userId);
        return Result.success(user);
    }

    @PutMapping("/info/update")
    public Result<Object> updateUserInfo(@RequestBody UserUpdateDTO userUpdateDTO){
        userService.updateUserInfo(userUpdateDTO);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Object> updateUserPassword(@RequestBody UserPwdDTO pwdDTO){
        userService.updateUserPassword(pwdDTO);
        return Result.success();
    }

    @GetMapping("/profile/{userId}")
    public Result<UserVO> queryUserProfile(@PathVariable Long userId){
        UserVO userVO = userService.queryUserProfile(userId);
        return Result.success(userVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(){
        userService.logout();
        return Result.success();
    }
}
