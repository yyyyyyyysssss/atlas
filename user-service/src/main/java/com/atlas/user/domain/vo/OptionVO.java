package com.atlas.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/26 10:34
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionVO<T extends Serializable> {

    private String label;

    private T value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T parentId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<OptionVO<T>> children;

    private Boolean selectable = Boolean.TRUE;

    public T getId() {
        return value;
    }

    public static <T extends Serializable> OptionVO<T> of(String label, T value) {
        return OptionVO.of(label, value, null);
    }

    public static <T extends Serializable> OptionVO<T> of(String label, T value, T parentId) {
        return OptionVO.<T>builder()
                .label(label)
                .value(value)
                .parentId(parentId)
                .build();
    }

    public static <S, T extends Serializable> List<OptionVO<T>> copyTree(
            List<S> treeNodes,
            Function<S, String> labelFunc,
            Function<S, T> valueFunc,
            Function<S, List<S>> childrenFunc) {

        if (treeNodes == null || treeNodes.isEmpty()) {
            return new ArrayList<>();
        }

        return treeNodes.stream().map(node -> {
            OptionVO<T> vo = OptionVO.of(labelFunc.apply(node), valueFunc.apply(node));
            // 递归转换子集
            vo.setChildren(copyTree(childrenFunc.apply(node), labelFunc, valueFunc, childrenFunc));
            return vo;
        }).collect(Collectors.toList());
    }

}
