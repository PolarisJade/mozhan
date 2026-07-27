package com.god.mz.domain.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket消息推送VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageVO<T> {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息数据
     */
    private T data;

    /**
     * 错误信息（错误类型时使用）
     */
    private String error;

    public WebSocketMessageVO(String type, T data) {
        this.type = type;
        this.data = data;
    }

    public static <T> WebSocketMessageVO<T> success(String type, T data) {
        return new WebSocketMessageVO<>(type, data);
    }

    public static WebSocketMessageVO<Void> error(String error) {
        WebSocketMessageVO<Void> vo = new WebSocketMessageVO<>();
        vo.setType("ERROR");
        vo.setError(error);
        return vo;
    }
}
