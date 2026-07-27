package com.god.mz.service.impl;

import com.god.mz.domain.po.ArticleTag;
import com.god.mz.mapper.ArticleTagMapper;
import com.god.mz.service.IArticleTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章标签关联表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag> implements IArticleTagService {

}
