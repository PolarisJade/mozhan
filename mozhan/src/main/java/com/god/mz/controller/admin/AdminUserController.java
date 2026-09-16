package com.god.mz.controller.admin;

import com.god.mz.domain.dto.UserLoginDTO;
import com.god.mz.domain.dto.admin.AdminUserStatusDTO;
import com.god.mz.domain.dto.admin.AdminUserUpdateDTO;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.user.AdminUserVO;
import com.god.mz.domain.vo.user.UserLoginVO;
import com.god.mz.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/user")
public class AdminUserController {
    @Resource
    private IUserService userService;

    @GetMapping("/page")
    public Result<PageQueryVO<AdminUserVO>> getUserPage(
                                @RequestParam Integer current,
                                @RequestParam Integer pageSize,
                                @RequestParam(required = false) String nickname,
                                @RequestParam(required = false) LocalDateTime start,
                                @RequestParam(required = false) LocalDateTime end) {
        PageQueryVO<AdminUserVO> vo = userService.getUserPage(current, pageSize, nickname, start, end);
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    public Result<AdminUserVO> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @PutMapping
    public Result<Void> updateUser(@RequestBody AdminUserUpdateDTO dto) {
        userService.updateUserByAdmin(dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeUserStatus(@PathVariable Long id, @RequestBody AdminUserStatusDTO dto) {
        userService.changeUserStatus(id, dto.getStatus());
        return Result.success();
    }

    @PostMapping("/login")
    public Result<UserLoginVO> adminLogin(@RequestBody UserLoginDTO userLoginDTO) {
        UserLoginVO vo = userService.login(userLoginDTO);
        return Result.success(vo);
    }
}
