package com.god.mz.domain.query.cursorQuery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageVO<T> {
    private List<T> list;
    private Boolean hasMore;
    private String nextCursor;
}
