package com.god.mz.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.exception.BizException;
import com.god.mz.service.IEmailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailServiceImpl implements IEmailService {
    @Resource
    private JavaMailSender javaMailSender;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 验证码有效期（分钟）
    private static final int CODE_EXPIRE_MINUTES = 5;

    // Redis key前缀
    private static final String EMAIL_CODE_PREFIX = "mozhan:email:code:";

    @Override
    public void sendVerificationCode(String email) {
        //生成6位随机验证码
        String code = RandomUtil.randomNumbers(6);

        //构造Redis key
        String redisKey = EMAIL_CODE_PREFIX + email;

        //将验证码保存到Redis中，设置过期时间
        stringRedisTemplate.opsForValue().set(redisKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        //构造邮件内容
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("墨栈用户注册验证码");
        message.setText("您的验证码是：" + code + "，有效期为" + CODE_EXPIRE_MINUTES + "分钟。请勿泄露给其他人。");

        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("邮件发送失败：{}", e.getMessage());
            throw new BizException(BizCodeEnum.EMAIL_SEND_FAILED);
        }

    }

    @Override
    public boolean verifyCode(String email, String code) {
        // 构造Redis key
        String redisKey = EMAIL_CODE_PREFIX + email;

        // 从Redis获取验证码
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        // 验证验证码
        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功后删除验证码，防止重复使用
            stringRedisTemplate.delete(redisKey);
            return true;
        }

        return false;
    }
}
