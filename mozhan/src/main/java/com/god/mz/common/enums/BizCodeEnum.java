package com.god.mz.common.enums;

import lombok.Getter;

@Getter
public enum BizCodeEnum {
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ERROR(1002, "账号或密码错误"),
    USER_NOT_AUTH(1003, "权限不足"),
    USER_DISABLED(1004, "账号已被禁用"),
    USER_PWD_ERROR(1005, "密码错误"),
    DATA_NOT_EXIST(1006, "数据不存在"),
    DATA_EXIST(1007, "数据已存在"),
    DATA_ERROR(1008, "请输入正确数据"),
    ARTICLE_NOT_EXIST(1009, "文章不存在"),
    COMMENT_NOT_EXIST(1010, "评论不存在"),
    OPERATION_FAILURE(1011, "操作失败，请稍后再试"),
    FILE_UPLOAD_FAILED(1012, "文件上传失败"),
    FILE_SIZE_EXCEEDED(1013, "文件大小超出限制"),
    FILE_FORMAT_NOT_SUPPORTED(1014, "文件格式不支持"),
    EMAIL_CODE_ERROR(1015, "邮箱验证码错误或已过期"),
    EMAIL_SEND_FAILED(1016, "邮件发送失败"),
    USER_EXIST(1017, "用户已存在"),
    EMAIL_HAS_USED(1018, "邮箱已被使用"),
    USER_INFO_CACHE_ERROR(1019, "用户信息缓存错误"),
    ESSAY_NOT_FOUND(1020, "随笔不存在"),
    CATEGORY_DELETE_ERROR(1021, "该分类有关联文章，不允许删除"),
    NOT_ADMIN(1022, "当前账号不是管理员，无法登录后台"),
    TAG_DELETE_ERROR(1023, "该标签有关联文章或随笔，不允许删除"),
    SEND_MESSAGE_ERROR(1024, "发送消息失败"),
    MESSAGE_NOT_EXIST(1025, "消息不存在"),
    MESSAGE_RECALL_FAILURE(1026, "消息发送超过2分钟，无法撤回"),
    SESSION_NOT_EXIST(1027, "会话不存在"),
    DIARY_EXISTS(1028, "一天只能够一篇日记");



    private final Integer code;
    private final String message;

    BizCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
