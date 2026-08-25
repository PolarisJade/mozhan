package com.god.mz.controller.user;

import com.god.mz.domain.dto.AIChatDTO;
import com.god.mz.domain.vo.ai.ChatEventVO;
import com.god.mz.service.AIChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody AIChatDTO dto) {
        return chatService.chat(dto.getQuestion(), dto.getSessionId());
    }
}
