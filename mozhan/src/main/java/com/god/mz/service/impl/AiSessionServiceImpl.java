package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.god.mz.config.AISessionProperties;
import com.god.mz.domain.po.AiSession;
import com.god.mz.domain.vo.ai.SessionVO;
import com.god.mz.mapper.AiSessionMapper;
import com.god.mz.service.IAiSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * AI会话表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-08-21
 */
@Service
@RequiredArgsConstructor
public class AiSessionServiceImpl extends ServiceImpl<AiSessionMapper, AiSession> implements IAiSessionService {

    private final AISessionProperties aiSessionProperties;

    @Override
    public SessionVO createSession(Integer num) {
        SessionVO vo = BeanUtil.toBean(aiSessionProperties, SessionVO.class);
        // 随机获取examples
        vo.setExamples(RandomUtil.randomEleList(aiSessionProperties.getExamples(), num));

        // 查看是否已有刚新建的对话
        Long userId = UserContext.getUserId();
        String title = aiSessionProperties.getTitle();
        AiSession existing = lambdaQuery()
                .eq(AiSession::getUserId, userId)
                .and(w -> w.isNull(AiSession::getTitle))
                .orderByAsc(AiSession::getId)
                .last("LIMIT 1")
                .one();
        if (existing != null) {
            // 已有未使用的会话，直接复用其sessionId
            vo.setSessionId(existing.getSessionId());
            return vo;
        }

        // 随机生成sessionId
        vo.setSessionId(IdUtil.simpleUUID());
        // 构建持久化对象，并持久化
        AiSession chatSession = AiSession.builder()
                .sessionId(vo.getSessionId())
                .userId(userId)
                .title(title)
                .build();

        save(chatSession);

        return vo;
    }

    @Override
    public List<SessionVO.Example> getHotProblem(Integer num) {
        return RandomUtil.randomEleList(aiSessionProperties.getExamples(), num);
    }
}
