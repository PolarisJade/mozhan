package com.god.mz.domain.query.cursorQuery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EssayCursorQuery extends CursorQuery{
    private List<Long> tagIdList;
}
