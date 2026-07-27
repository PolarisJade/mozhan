package com.god.mz.domain.query.cursorQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SessionCursorQuery extends CursorQuery{
    private String sortBy = "last_message_time";
}
