package com.god.mz.controller.user;

import com.god.mz.domain.dto.aiWriter.AIWriterArticleDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterMetaDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterOutlineDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterReviseDTO;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.ai.ChatEventVO;
import com.god.mz.domain.vo.ai.aiWriter.ArticleMetaVO;
import com.god.mz.service.AIWriterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai/writer") @RequiredArgsConstructor
public class AIWriterController {
private final AIWriterService writerService;

/**
 * 生成大纲（非流式）
 */
@PostMapping("/outline")
public Result<String> outline(@RequestBody AIWriterOutlineDTO dto) {
    return Result.success(writerService.generateOutline(dto));
}

/**
 * 生成正文（流式 HTML）
 */
@PostMapping(value = "/article", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ChatEventVO> article(@RequestBody AIWriterArticleDTO dto) {
    return writerService.generateArticle(dto);
}

/**
 * 修改正文（流式 HTML，携带编辑器当前正文 + 修改要求）
 */
@PostMapping(value = "/revise", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ChatEventVO> revise(@RequestBody AIWriterReviseDTO dto) {
    return writerService.reviseArticle(dto);
}

/**
 * 生成标题与摘要（非流式）
 */
@PostMapping("/meta")
public Result<ArticleMetaVO> meta(@RequestBody AIWriterMetaDTO dto) {
    return Result.success(writerService.generateMeta(dto));
}
}