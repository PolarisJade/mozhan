package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.enums.LikeTypeEnum;
import com.god.mz.domain.dto.EssayDTO;
import com.god.mz.domain.po.*;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.CursorQuery;
import com.god.mz.domain.query.cursorQuery.EssayCursorQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.essay.EssayVO;
import com.god.mz.domain.vo.tag.TagVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.EssayMapper;
import com.god.mz.mapper.EssayTagMapper;
import com.god.mz.mapper.TagMapper;
import com.god.mz.service.IEssayService;
import com.god.mz.service.ILikeService;
import com.god.mz.service.IUserService;
import com.god.mz.util.CursorCodeUtil;
import com.god.mz.util.CursorQueryUtil;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EssayServiceImpl extends ServiceImpl<EssayMapper, Essay> implements IEssayService {

    private final EssayTagMapper essayTagMapper;
    private final ILikeService likeService;
    private final IUserService userService;
    private final TagMapper tagMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    @Override
    public void addEssay(EssayDTO essayDTO) {
        Essay essay = BeanUtil.copyProperties(essayDTO, Essay.class);
        essay.setAuthorId(UserContext.getUserId());

        boolean success = save(essay);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        //保存随笔标签
        if (essayDTO.getTagIdList() != null && !essayDTO.getTagIdList().isEmpty()) {
            List<EssayTag> essayTagList = essayDTO.getTagIdList().stream()
                    .map(tagId -> new EssayTag(null, essay.getId(), tagId)).toList();
            essayTagMapper.insert(essayTagList);
        }
    }

    public EssayVO getEssayDetail(Long id) {
        Essay essay = getById(id);
        if (essay == null){
            throw new BizException(BizCodeEnum.ESSAY_NOT_FOUND);
        }

        EssayVO vo = convertToVO(essay);

        String key = LikeTypeEnum.essay.getSetKey(id);
        Long redisCount = stringRedisTemplate.opsForSet().size(key);
        if (redisCount != null && redisCount > 0) {
            vo.setLikeCount(redisCount);
        }
        vo.setIsLike(likeService.isLiked(LikeTypeEnum.essay, id));
        return vo;
    }

    @Override
    public CursorPageVO<EssayVO> getMyEssayList(Long userId, EssayCursorQuery query) {
        QueryWrapper<Essay> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("author_id", userId);

        return buildEssayPage(queryWrapper, query);
    }

    @Override
    @Transactional
    public void deleteEssay(Long id) {
        Essay essay = getById(id);
        if (essay == null){
            throw new BizException(BizCodeEnum.ESSAY_NOT_FOUND);
        }

        if (!Objects.equals(essay.getAuthorId(), UserContext.getUserId())){
            throw new BizException(405, "非随笔作者不可删除");
        }

        boolean success = removeById(essay);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        //级联删除标签关联
        essayTagMapper.delete(new QueryWrapper<EssayTag>()
                .eq("essay_id", id));
    }

    @Override
    public CursorPageVO<EssayVO> getEssayList(EssayCursorQuery query) {
        QueryWrapper<Essay> queryWrapper = new QueryWrapper<>();

        List<Long> tagIdList = query.getTagIdList();
        if (tagIdList != null && !tagIdList.isEmpty()) {
            List<Long> essayIds = essayTagMapper.selectList(
                    new QueryWrapper<EssayTag>().in("tag_id", tagIdList)
            ).stream().map(EssayTag::getEssayId).distinct().toList();

            if (essayIds.isEmpty()) {
                return new CursorPageVO<>(List.of(), false, null);
            }
            queryWrapper.in("id", essayIds);
        }

        return buildEssayPage(queryWrapper, query);
    }

    @Override
    public void updateEssay(EssayDTO essayDTO) {
        Essay essay = getById(essayDTO.getId());
        if (essay == null) {
            throw new BizException(BizCodeEnum.ESSAY_NOT_FOUND);
        }

        if (!Objects.equals(essay.getAuthorId(), UserContext.getUserId())) {
            throw new BizException(405, "非随笔作者不可修改");
        }

        essay.setContent(essayDTO.getContent());
        boolean success = updateById(essay);
        if (!success) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        // 更新标签关联：先删后增
        essayTagMapper.delete(new QueryWrapper<EssayTag>()
                .eq("essay_id", essayDTO.getId()));
        if (essayDTO.getTagIdList() != null && !essayDTO.getTagIdList().isEmpty()) {
            List<EssayTag> essayTagList = essayDTO.getTagIdList().stream()
                    .map(tagId -> new EssayTag(null, essayDTO.getId(), tagId)).toList();
            essayTagMapper.insert(essayTagList);
        }
    }

    private EssayVO convertToVO(Essay essay) {
        EssayVO vo = BeanUtil.copyProperties(essay, EssayVO.class);

        User author = userService.getById(essay.getAuthorId());
        vo.setAuthorName(author != null ? author.getNickname() : null);
        vo.setAvatar(author != null ? author.getAvatar() : null);

        List<EssayTag> essayTagList = essayTagMapper.selectList(
                new QueryWrapper<EssayTag>().eq("essay_id", essay.getId())
        );

        if (essayTagList != null && !essayTagList.isEmpty()) {
            List<Long> tagIds = essayTagList.stream()
                    .map(EssayTag::getTagId)
                    .toList();

            List<Tag> tagList = tagMapper.selectByIds(tagIds);

            List<TagVO> tagVOList = tagList.stream()
                    .map(tag -> new TagVO(tag.getId(), tag.getName()))
                    .toList();

            vo.setTagVOList(tagVOList);
        } else {
            vo.setTagVOList(List.of());
        }

        return vo;
    }

    private CursorPageVO<EssayVO> buildEssayPage(QueryWrapper<Essay> queryWrapper, CursorQuery query) {
        Integer pageSize = query.getPageSize();
        String sortBy = query.getSortBy();

        CursorQueryUtil.applyCursor(queryWrapper, query, "create_time", "id");

        List<Essay> essayList = list(queryWrapper);

        boolean hasMore = essayList.size() > pageSize;
        if (hasMore) {
            essayList = essayList.subList(0, pageSize);
        }

        List<EssayVO> essayVOList = essayList.stream().map(this::convertToVO).toList();

        // 批量填充点赞信息（是否点赞 + 点赞数兜底）
        fillLikeInfo(essayVOList);

        List<Long> cursors = CursorQueryUtil.getNextCursor(essayList, sortBy, "create_time", "id");
        return new CursorPageVO<>(essayVOList, hasMore, CursorCodeUtil.encode(cursors));
    }

    private void fillLikeInfo(List<EssayVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        List<Long> essayIds = voList.stream().map(EssayVO::getId).toList();

        // 批量查询当前用户是否点赞（likeService 内部已处理未登录情况）
        Set<Long> likedSet = likeService.isLiked(LikeTypeEnum.essay, essayIds);
        if (likedSet == null) {
            likedSet = Set.of();
        }

        for (EssayVO vo : voList) {
            vo.setIsLike(likedSet.contains(vo.getId()));
            if (vo.getLikeCount() == null) {
                vo.setLikeCount(0L);
            }
        }
    }

    @Override
    public PageQueryVO<EssayVO> queryEssayPage(Integer pageNum, Integer pageSize, Long authorId) {
        Page<Essay> page = new Page<>(pageNum, pageSize);

        QueryWrapper<Essay> queryWrapper = new QueryWrapper<>();

        if (authorId != null) {
            queryWrapper.eq("author_id", authorId);
        }

        queryWrapper.orderByDesc("create_time");

        IPage<Essay> essayPage = page(page, queryWrapper);

        List<Long> authorIds = essayPage.getRecords().stream()
                .map(Essay::getAuthorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            userMap = userService.listByIds(authorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        }

        List<Long> essayIds = essayPage.getRecords().stream()
                .map(Essay::getId)
                .collect(Collectors.toList());

        Map<Long, List<Tag>> essayTagMap = new HashMap<>();
        if (!essayIds.isEmpty()) {
            List<EssayTag> essayTagList = essayTagMapper.selectList(
                    new QueryWrapper<EssayTag>().in("essay_id", essayIds)
            );

            if (!essayTagList.isEmpty()) {
                List<Long> tagIds = essayTagList.stream()
                        .map(EssayTag::getTagId)
                        .distinct()
                        .collect(Collectors.toList());

                List<Tag> tagList = tagMapper.selectByIds(tagIds);
                Map<Long, Tag> tagMap = tagList.stream()
                        .collect(Collectors.toMap(Tag::getId, t -> t));

                essayTagMap = essayTagList.stream()
                        .filter(et -> tagMap.containsKey(et.getTagId()))
                        .collect(Collectors.groupingBy(
                                EssayTag::getEssayId,
                                Collectors.mapping(
                                        et -> tagMap.get(et.getTagId()),
                                        Collectors.toList()
                                )
                        ));
            }
        }

        final Map<Long, User> finalUserMap = userMap;
        final Map<Long, List<Tag>> finalEssayTagMap = essayTagMap;

        List<EssayVO> voList = essayPage.getRecords().stream().map(essay -> {
            EssayVO vo = BeanUtil.copyProperties(essay, EssayVO.class);

            User author = finalUserMap.get(essay.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAvatar(author.getAvatar());
            } else {
                vo.setAuthorName("未知用户");
                vo.setAvatar("");
            }

            List<Tag> tags = finalEssayTagMap.getOrDefault(essay.getId(), new ArrayList<>());
            List<TagVO> tagVOList = tags.stream()
                    .map(tag -> new TagVO(tag.getId(), tag.getName()))
                    .collect(Collectors.toList());
            vo.setTagVOList(tagVOList);

            return vo;
        }).collect(Collectors.toList());

        PageQueryVO<EssayVO> result = new PageQueryVO<>();
        result.setRecords(voList);
        result.setTotal(essayPage.getTotal());
        result.setPageSize(essayPage.getSize());
        result.setPageNo(essayPage.getCurrent());
        result.setPages(essayPage.getPages());

        return result;
    }

    @Override
    @Transactional
    public void updateLikeCount(int maxSize) {
        String key = LikeTypeEnum.essay.getCountKey();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().popMin(key, maxSize);
        if (typedTuples == null) return;

        List<Essay> list = new ArrayList<>(typedTuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            String targetId = tuple.getValue();
            Double count = tuple.getScore();
            if (targetId == null || count == null) continue;
            Essay essay = new Essay();
            essay.setId(Long.parseLong(targetId));
            essay.setLikeCount(count.longValue());
            list.add(essay);
        }
        updateBatchById(list);
    }

}
