package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.common.enums.UserStatusEnum;
import com.god.mz.common.enums.UserTypeEnum;
import com.god.mz.domain.dto.UserLoginDTO;
import com.god.mz.domain.dto.UserPwdDTO;
import com.god.mz.domain.dto.UserUpdateDTO;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.User;
import com.god.mz.domain.po.UserFollow;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.user.AdminUserVO;
import com.god.mz.domain.vo.user.UserLoginVO;
import com.god.mz.domain.vo.user.UserVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.ArticleMapper;
import com.god.mz.mapper.EssayMapper;
import com.god.mz.mapper.UserFollowMapper;
import com.god.mz.mapper.UserMapper;
import com.god.mz.service.IEmailService;
import com.god.mz.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.JWTUtil;
import com.god.mz.util.PasswordUtil;
import com.god.mz.util.UserContext;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private IEmailService emailService;
    @Resource
    private UserFollowMapper userFollowMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private EssayMapper essayMapper;




    /**
     * 根据id查询用户信息
     * @param userId 用户id
     * @return 用户信息
     */
    @Override
    public User queryUserInfo(Long userId) {
        if (userId == null){
            throw new BizException(BizCodeEnum.DATA_ERROR);
        }
        User user = lambdaQuery().eq(User::getId, userId).one();
        if (user == null){
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 修改用户信息
     * @param userUpdateDTO 要修改的用户信息
     */
    @Override
    public void updateUserInfo(UserUpdateDTO userUpdateDTO) {
        //获取当前用户id
        Long userId = UserContext.getUserId();
        if (userId == null){
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        User user = BeanUtil.copyProperties(userUpdateDTO, User.class);
        user.setId(userId);
        boolean success = updateById(user);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
        stringRedisTemplate.delete(RedisConstant.USER_PROFILE_KEY_PREFIX + userId);
    }

    /**
     * 修改密码
     * @param pwdDTO 新密码和旧密码
     */
    @Override
    public void updateUserPassword(UserPwdDTO pwdDTO) {
        //获取当前用户id
        Long userId = UserContext.getUserId();
        if (userId == null){
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        User user = lambdaQuery().eq(User::getId, userId).one();
        if (PasswordUtil.matches(pwdDTO.getOldPassword(), user.getPassword())){
            throw new BizException(BizCodeEnum.USER_PWD_ERROR);
        }
        user.setPassword(PasswordUtil.encode(pwdDTO.getNewPassword()));
        boolean success = updateById(user);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
    }

    @Override
    public UserVO queryUserProfile(Long userId) {
        // 1. 尝试从 Redis 获取缓存（仅公共数据，不含 isFollowed）
        String key = RedisConstant.USER_PROFILE_KEY_PREFIX + userId;
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                UserVO vo = objectMapper.readValue(json, UserVO.class);
                fillIsFollowed(vo, userId);
                return vo;
            }
        } catch (JsonProcessingException e) {
            // 缓存数据异常，降级查库
        }

        // 2. 缓存未命中，从数据库查询
        UserVO userVO = buildUserProfileFromDb(userId);

        // 3. 写入 Redis（不缓存 isFollowed）
        try {
            userVO.setIsFollowed(null);
            String json = objectMapper.writeValueAsString(userVO);
            stringRedisTemplate.opsForValue().set(key, json, 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            // 写入失败不影响响应
        }

        // 4. 补充 isFollowed 后返回
        fillIsFollowed(userVO, userId);
        return userVO;
    }

    /**
     * 用户登录
     * @param userLoginDTO 用户信息
     * @return 登录结果
     */
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        // 根据用户名查询用户
        User user = lambdaQuery()
                .eq(User::getUsername, userLoginDTO.getUsername())
                .one();
        if (user == null) {
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);
        }

        // 验证密码（BCrypt 密文匹配）
        if (PasswordUtil.matches(userLoginDTO.getPassword(), user.getPassword())) {
            throw new BizException(BizCodeEnum.USER_PWD_ERROR);
        }

        // 验证账号状态
        if (user.getStatus() == UserStatusEnum.DISABLE) {
            throw new BizException(BizCodeEnum.USER_DISABLED);
        }

        // 管理端登录需验证管理员身份
        if (Boolean.TRUE.equals(userLoginDTO.getAdmin()) && user.getAdmin() != UserTypeEnum.ENABLE) {
            throw new BizException(BizCodeEnum.NOT_ADMIN);
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId());

        // 构建返回数据
        return new UserLoginVO(token, user.getId(), user.getNickname(), user.getAvatar());
    }

    @Override
    public void logout() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        // 清除 Redis 中的用户信息缓存
        stringRedisTemplate.delete(RedisConstant.USER_PROFILE_KEY_PREFIX + userId);


        // 将当前 token 加入黑名单，TTL 与 JWT 最大有效期一致（24小时）
        String token = UserContext.getToken();
        if (token != null) {
            String blacklistKey = RedisConstant.TOKEN_BLACKLIST_PREFIX + token;
            stringRedisTemplate.opsForValue()
                    .set(blacklistKey, "1", RedisConstant.USER_INFO_EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }

    @Override
    public PageQueryVO<AdminUserVO> getUserPage(Integer current, Integer pageSize, String nickname, LocalDateTime start, LocalDateTime end) {
        // 1. 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(nickname)) {
            queryWrapper.like("nickname", nickname);
        }
        if (start != null) {
            queryWrapper.ge("create_time", start);
        }
        if (end != null) {
            queryWrapper.le("create_time", end);
        }
        queryWrapper.orderByDesc("create_time");

        // 2. 执行分页查询
        Page<User> page = new Page<>(current != null ? current : 1, pageSize != null ? pageSize : 10);
        Page<User> result = page(page, queryWrapper);

        if (result.getRecords().isEmpty()) {
            return new PageQueryVO<>(Collections.emptyList(), 0L, page.getSize(), page.getCurrent(), 0L);
        }

        // 3. 收集用户ID
        List<Long> userIds = result.getRecords().stream().map(User::getId).collect(Collectors.toList());

        // 4. 批量查询文章和随笔统计
        Map<Long, Long> articleCountMap = articleMapper.selectUserArticleCounts(userIds).stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("authorId")).longValue(),
                        m -> ((Number) m.get("total")).longValue()
                ));

        Map<Long, Long> essayCountMap = essayMapper.selectUserEssayCounts(userIds).stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("authorId")).longValue(),
                        m -> ((Number) m.get("total")).longValue()
                ));

        // 5. 转换为 AdminUserVO
        List<AdminUserVO> records = result.getRecords().stream().map(user -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(user.getId());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            vo.setArticleCount(articleCountMap.getOrDefault(user.getId(), 0L));
            vo.setEssayCount(essayCountMap.getOrDefault(user.getId(), 0L));
            return vo;
        }).collect(Collectors.toList());

        // 6. 返回分页结果
        return new PageQueryVO<>(records, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    /**
     * 用户注册
     * @param userLoginDTO 用户注册信息
     * @return 注册结果
     */
    public UserLoginVO register(UserLoginDTO userLoginDTO) {
        // 验证邮箱验证码
        if (!emailService.verifyCode(userLoginDTO.getEmail(), userLoginDTO.getVerificationCode())) {
            throw new BizException(BizCodeEnum.EMAIL_CODE_ERROR);
        }

        // 检查用户名是否已存在
        User existingUser = lambdaQuery()
                .eq(User::getUsername, userLoginDTO.getUsername())
                .one();
        if (existingUser != null) {
            throw new BizException(BizCodeEnum.USER_EXIST);
        }

        // 检查邮箱是否已存在
        existingUser = lambdaQuery()
                .eq(User::getEmail, userLoginDTO.getEmail())
                .one();
        if (existingUser != null) {
            throw new BizException(BizCodeEnum.EMAIL_HAS_USED);
        }

        // 创建新用户
        User newUser = BeanUtil.copyProperties(userLoginDTO, User.class);
        newUser.setPassword(PasswordUtil.encode(userLoginDTO.getPassword()));
        newUser.setStatus(UserStatusEnum.ENABLE);
        newUser.setIntro("这个人很懒，什么都没写");
        // 保存用户
        save(newUser);

        // 生成token
        String token = jwtUtil.generateToken(newUser.getId());

        // 构建返回数据
        return new UserLoginVO(token, newUser.getId(), newUser.getNickname(), newUser.getAvatar());
    }

    private UserVO buildUserProfileFromDb(Long userId) {
        User user = lambdaQuery().eq(User::getId, userId).one();
        if (user == null) {
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);
        }
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);

        Long followingCount = userFollowMapper.selectCount(new QueryWrapper<UserFollow>().eq("user_id", userId));
        userVO.setFollowingCount(Math.toIntExact(followingCount));

        Long followerCount = userFollowMapper.selectCount(new QueryWrapper<UserFollow>().eq("follow_id", userId));
        userVO.setFollowerCount(Math.toIntExact(followerCount));

        Long articleCount = articleMapper.selectCount(new QueryWrapper<Article>()
                .eq("author_id", userId)
                .eq("del_flag", false));
        userVO.setArticleCount(Math.toIntExact(articleCount));

        return userVO;
    }

    private void fillIsFollowed(UserVO vo, Long profileUserId) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            vo.setIsFollowed(false);
        } else if (currentUserId.equals(profileUserId)) {
            vo.setIsFollowed(false);
        } else {
            boolean exists = userFollowMapper.exists(new QueryWrapper<UserFollow>()
                    .eq("user_id", currentUserId)
                    .eq("follow_id", profileUserId));
            vo.setIsFollowed(exists);
        }
    }

}