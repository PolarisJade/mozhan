package com.god.mz.domain.query.cursorQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MsgCursorQuery extends CursorQuery{
    private Long sessionId;
}
