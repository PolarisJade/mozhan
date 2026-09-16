package com.god.mz.service;

import com.god.mz.common.enums.UserStatusEnum;
import com.god.mz.domain.dto.UserLoginDTO;
import com.god.mz.domain.dto.UserPwdDTO;
import com.god.mz.domain.dto.UserUpdateDTO;
import com.god.mz.domain.dto.admin.AdminUserUpdateDTO;
import com.god.mz.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.user.AdminUserVO;
import com.god.mz.domain.vo.user.UserLoginVO;
import com.god.mz.domain.vo.user.UserVO;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface IUserService extends IService<User> {

    User queryUserInfo(Long userId);

    void updateUserInfo(UserUpdateDTO userUpdateDTO);

    void updateUserPassword(UserPwdDTO pwdDTO);

    UserVO queryUserProfile(Long userId);

    UserLoginVO register(UserLoginDTO userLoginDTO);

    UserLoginVO login(UserLoginDTO userLoginDTO);

    void logout();

    PageQueryVO<AdminUserVO> getUserPage(Integer current, Integer pageSize, String nickname, LocalDateTime start, LocalDateTime end);

    /**
     * 后台：查询单个用户详情（含文章数/随笔数）
     */
    AdminUserVO getUserDetail(Long userId);

    /**
     * 后台：编辑用户资料（昵称/邮箱/头像/简介）。不含密码与状态。
     */
    void updateUserByAdmin(AdminUserUpdateDTO dto);

    /**
     * 后台：启用或禁用账号
     */
    void changeUserStatus(Long userId, UserStatusEnum status);
}
