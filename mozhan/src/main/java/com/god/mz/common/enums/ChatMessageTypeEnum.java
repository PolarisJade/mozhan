package com.god.mz.common.enums;

import lombok.Getter;

/**
 * WebSocket message type enum
 */
@Getter
public enum ChatMessageTypeEnum {
    CHAT("CHAT", "Send message"),
    RECALL("RECALL", "Recall message"),
    READ("READ", "Read receipt"),
    NEW_MESSAGE("NEW_MESSAGE", "New message push"),
    RECALL_NOTICE("RECALL_NOTICE", "Recall notice"),
    ERROR("ERROR", "Error message"),
    PING("PING", "Heartbeat ping");

    private final String code;
    private final String desc;

    ChatMessageTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
