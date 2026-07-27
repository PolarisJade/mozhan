package com.god.mz.domain.vo.comment;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private Long id;

    private Long parentId;

    private Long replyToId;

    private Long userId;

    private String nickname;

    private String avatar;

    private String content;

    private Boolean isAuthor;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private String replyToNickname;

    private List<CommentVO> replies;

    private Integer totalReplies;
}
