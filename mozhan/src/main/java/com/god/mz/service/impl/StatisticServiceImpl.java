package com.god.mz.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.enums.ArticleStatusEnum;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.common.enums.UserStatusEnum;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.User;
import com.god.mz.domain.vo.statistic.StatisticVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.ArticleMapper;
import com.god.mz.mapper.EssayMapper;
import com.god.mz.mapper.TagMapper;
import com.god.mz.mapper.UserMapper;
import com.god.mz.service.IStatisticService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class StatisticServiceImpl implements IStatisticService {
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TagMapper tagMapper;
    @Resource
    private EssayMapper essayMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public StatisticVO getStatistics() {
        //尝试从redis中获取
        try {
            String json = stringRedisTemplate.opsForValue().get(RedisConstant.STATISTIC_KEY);
            if (StrUtil.isNotBlank(json)){
                return objectMapper.readValue(json, StatisticVO.class);
            }
        } catch (JsonProcessingException e) {
            throw new BizException(500, "获取统计数据失败");
        }

        //从数据库中获取
        StatisticVO vo = new StatisticVO();
        vo.setArticleCount(articleMapper.selectCount(new QueryWrapper<Article>()
                        .eq("status", ArticleStatusEnum.PUBLISHED)
                        .eq("del_flag", false)));

        vo.setUserCount(userMapper.selectCount(new QueryWrapper<User>()
                .eq("status", UserStatusEnum.ENABLE)));

        vo.setTagCount(tagMapper.selectCount(new QueryWrapper<>()));

        vo.setEssayCount(essayMapper.selectCount(new QueryWrapper<>()));

        //保存到redis中
        try {
            String json = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForValue().set(RedisConstant.STATISTIC_KEY, json, RedisConstant.DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            throw new BizException(500, "保存统计数据失败");
        }

        return vo;
    }
}
