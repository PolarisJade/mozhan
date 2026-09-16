package com.god.mz.domain.vo.comment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台评论列表项，天然是两级树。
 * <p>
 * 没有 extends {@link CommentVO}：父类的 {@code replies} 是 {@code List<CommentVO>}，
 * 子类无法把元素类型收窄成 AdminCommentVO，硬继承就只能拿到一个字段不全的嵌套列表，
 * 等于后面还要再拼一次。不如各管各的。
 * </p>
 */
@Data
public class AdminCommentVO {
    private Long id;

    /**
     * 一级评论恒为 0；回复指向所属一级评论的 id。
     */
    private Long parentId;

    private Long replyToId;

    /**
     * 被回复人的昵称。被回复的评论已删除时为「评论已删除」。
     */
    private String replyToNickname;

    private Long userId;

    private String nickname;

    private String avatar;

    private String content;

    private Long articleId;

    private String articleTitle;

    /**
     * 软删除标记。后台列表只查未删除的，保留这个字段是为了前端能明确展示状态而不是靠猜。
     */
    private Boolean delFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 二级：回复。仅一级评论有值，回复的该字段恒为空列表。
     */
    private List<AdminCommentVO> replies;

    private Integer totalReplies;
}
