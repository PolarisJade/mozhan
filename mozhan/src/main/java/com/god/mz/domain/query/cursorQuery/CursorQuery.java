package com.god.mz.domain.query.cursorQuery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorQuery {
    private Integer pageSize = 20;

    private Boolean isAsc = false;

    private String sortBy = "create_time";

    private String nextCursor;
}
