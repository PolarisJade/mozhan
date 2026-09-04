package com.god.mz.domain.query.cursorQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AISessionCursorQuery extends CursorQuery {

    private String sortBy = "update_time";
}