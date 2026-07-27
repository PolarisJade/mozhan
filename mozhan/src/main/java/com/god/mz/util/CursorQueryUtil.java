package com.god.mz.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.god.mz.domain.query.cursorQuery.CursorQuery;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CursorQueryUtil {

    public static <T> void applyCursor(QueryWrapper<T> queryWrapper,
                                        CursorQuery query,
                                        String... secondarySorts) {
        List<Long> cursors = CursorCodeUtil.decode(query.getNextCursor());
        if (cursors == null || cursors.isEmpty()) {
            addOrderBy(queryWrapper, query.getSortBy(), query.getIsAsc(), secondarySorts);
            queryWrapper.last("LIMIT " + (query.getPageSize() + 1));
            return;
        }

        String[] allFields = buildFieldArray(query.getSortBy(), secondarySorts);
        boolean asc = query.getIsAsc();

        buildCompositeWhere(queryWrapper, allFields, cursors, asc);
        addOrderBy(queryWrapper, query.getSortBy(), asc, secondarySorts);
        queryWrapper.last("LIMIT " + (query.getPageSize() + 1));
    }

    /**
     * 获取复合游标值 [主游标, 次游标1, ...]
     */
    public static <T> List<Long> getNextCursor(List<T> list, String sortBy, String... secondarys) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        T lastItem = list.getLast();
        List<Long> cursors = new ArrayList<>();
        cursors.add(extractCursorValue(lastItem, sortBy));
        for (String field : secondarys) {
            cursors.add(extractCursorValue(lastItem, field));
        }
        return cursors;
    }

    private static <T> void buildCompositeWhere(QueryWrapper<T> wrapper,
                                                 String[] fields, List<Long> cursors,
                                                 boolean asc) {
        wrapper.and(w -> {
            if (asc) {
                w.gt(fields[0], cursors.getFirst());
            } else {
                w.lt(fields[0], cursors.getFirst());
            }
            for (int i = 1; i < fields.length && i < cursors.size(); i++) {
                int level = i;
                w.or(sub -> {
                    for (int j = 0; j < level; j++) {
                        sub.eq(fields[j], cursors.get(j));
                    }
                    if (asc) {
                        sub.gt(fields[level], cursors.get(level));
                    } else {
                        sub.lt(fields[level], cursors.get(level));
                    }
                });
            }
        });
    }

    private static <T> void addOrderBy(QueryWrapper<T> wrapper, String sortBy,
                                        boolean asc, String... secondarys) {
        if (asc) {
            wrapper.orderByAsc(sortBy);
        } else {
            wrapper.orderByDesc(sortBy);
        }
        for (String field : secondarys) {
            if (asc) {
                wrapper.orderByAsc(field);
            } else {
                wrapper.orderByDesc(field);
            }
        }
    }

    private static String[] buildFieldArray(String sortBy, String... secondarys) {
        String[] fields = new String[secondarys.length + 1];
        fields[0] = sortBy;
        System.arraycopy(secondarys, 0, fields, 1, secondarys.length);
        return fields;
    }

    private static <T> Long extractCursorValue(T item, String columnName) {
        String propertyName = StrUtil.toCamelCase(columnName);
        Object value = BeanUtil.getProperty(item, propertyName);
        return switch (value) {
            case LocalDateTime dateTime -> dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case Number number -> number.longValue();
            case null, default -> null;
        };
    }
}
