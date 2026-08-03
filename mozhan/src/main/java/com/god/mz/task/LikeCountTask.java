package com.god.mz.task;

import com.god.mz.service.IArticleService;
import com.god.mz.service.IEssayService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeCountTask {

    private final IArticleService articleService;
    private final IEssayService essayService;


    @Scheduled(fixedDelay = 20000)
    public void updateLikeInfo() {
        int maxSize = 30;
        articleService.updateLikeCount(maxSize);
        essayService.updateLikeCount(maxSize);
    }
}
