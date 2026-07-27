package com.god.mz.controller.user;

import com.god.mz.domain.vo.Result;
import com.god.mz.service.IEmailService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/email")
public class EmailController {
    @Resource
    private IEmailService emailService;

    @PostMapping("/sendCode")
    public Result<Void> sendCode(@RequestBody Map<String, String> body){
        String email = body.get("email");
        emailService.sendVerificationCode(email);
        return Result.success();
    }
}
