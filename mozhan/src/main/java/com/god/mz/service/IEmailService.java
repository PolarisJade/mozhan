package com.god.mz.service;

public interface IEmailService {
    void sendVerificationCode(String email);

    boolean verifyCode(String email, String verificationCode);
}
