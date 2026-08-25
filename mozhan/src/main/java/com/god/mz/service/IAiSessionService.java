package com.god.mz.service;

import com.god.mz.domain.po.AiSession;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.vo.ai.SessionVO;

import java.util.List;

/**
 * <p>
 * AI会话表 服务类
 * </p>
 *
 * @author God
 * @since 2026-08-21
 */
public interface IAiSessionService extends IService<AiSession> {

    SessionVO createSession(Integer num);

    List<SessionVO.Example> getHotProblem(Integer num);
}
