package com.god.mz.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.enums.ArticleStatusEnum;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.enums.UserStatusEnum;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.User;
import com.god.mz.domain.vo.statistic.AdminStatisticVO;
import com.god.mz.domain.vo.statistic.DailyCountVO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class StatisticServiceImpl implements IStatisticService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 逐日序列由后端按天补齐，区间过大时会产生海量数据点。一年足够覆盖「自定义区间」的合理用法。
     */
    private static final long MAX_RANGE_DAYS = 366;

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

    @Override
    public AdminStatisticVO getAdminOverview(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        LocalDate from = start != null ? start : today;
        LocalDate to = end != null ? end : today;
        if (to.isBefore(from)) {
            throw new BizException(BizCodeEnum.DATA_ERROR.getCode(), "结束日期不能早于开始日期");
        }
        // 逐日序列是后端按天补齐的，区间给太大（比如误传 2000-01-01）会平白造出上万个数据点
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BizException(BizCodeEnum.DATA_ERROR.getCode(), "查询区间不能超过 " + MAX_RANGE_DAYS + " 天");
        }

        // 左闭右开：结束日当天的数据带着时分秒，用 <= end 会把 00:00:00 之后的全部漏掉
        LocalDateTime startTime = from.atStartOfDay();
        LocalDateTime endTime = to.plusDays(1).atStartOfDay();

        Map<String, Long> userDaily = countByDay(userMapper, startTime, endTime, false);
        Map<String, Long> articleDaily = countByDay(articleMapper, startTime, endTime, true);
        Map<String, Long> essayDaily = countByDay(essayMapper, startTime, endTime, false);

        List<DailyCountVO> trend = new ArrayList<>();
        long newUser = 0L;
        long newArticle = 0L;
        long newEssay = 0L;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String key = d.format(DATE_FORMATTER);
            long u = userDaily.getOrDefault(key, 0L);
            long a = articleDaily.getOrDefault(key, 0L);
            long e = essayDaily.getOrDefault(key, 0L);
            trend.add(new DailyCountVO(key, u, a, e));
            newUser += u;
            newArticle += a;
            newEssay += e;
        }

        AdminStatisticVO vo = new AdminStatisticVO();
        vo.setStartDate(from.format(DATE_FORMATTER));
        vo.setEndDate(to.format(DATE_FORMATTER));
        vo.setDailyTrend(trend);
        vo.setNewUserCount(newUser);
        vo.setNewArticleCount(newArticle);
        vo.setNewEssayCount(newEssay);

        // 累计量刻意不走 Redis 缓存：前台那个 getStatistics 缓存的是另一套口径，
        // 混在一起用会导致工作台数字和用户管理列表对不上。
        vo.setTotalUserCount(userMapper.selectCount(new QueryWrapper<>()));
        vo.setTotalArticleCount(articleMapper.selectCount(
                new QueryWrapper<Article>().eq("del_flag", false)));
        vo.setTotalEssayCount(essayMapper.selectCount(new QueryWrapper<>()));

        return vo;
    }

    /**
     * 按天分组统计。用 MyBatis-Plus 的 selectMaps + groupBy 就够了，不必为此写 XML。
     *
     * @param filterDeleted 是否排除软删除行。目前只有 article 表有 del_flag；
     *                      users / essay 没有这一列，传 true 会直接 SQL 报错。
     */
    private <T> Map<String, Long> countByDay(BaseMapper<T> mapper, LocalDateTime startTime,
            LocalDateTime endTime, boolean filterDeleted) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(create_time) AS stat_date", "COUNT(*) AS stat_count")
                .ge("create_time", startTime)
                .lt("create_time", endTime)
                .isNotNull("create_time");
        if (filterDeleted) {
            wrapper.eq("del_flag", false);
        }
        wrapper.groupBy("DATE(create_time)");

        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> row : mapper.selectMaps(wrapper)) {
            Object date = row.get("stat_date");
            Object count = row.get("stat_count");
            if (date == null || count == null) {
                continue;
            }
            // DATE() 在 MySQL 驱动下回来的是 java.sql.Date，toString 刚好是 yyyy-MM-dd
            result.put(date.toString(), ((Number) count).longValue());
        }
        return result;
    }
}
