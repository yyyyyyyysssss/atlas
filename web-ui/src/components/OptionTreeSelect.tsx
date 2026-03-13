import { Spin, TreeSelect, TreeSelectProps } from "antd";
import React, { useEffect, useMemo, useState } from "react";
import { useRequest } from 'ahooks';

// 定义字段映射的接口
export interface TreeFieldNames {
    id?: string;     // 对应唯一 ID 字段
    label?: string;  // 对应显示的名称字段
    children?: string; // 对应子节点字段
}

type TreeSelectPropsType<T = any> = Omit<
    TreeSelectProps,
    "value" | "onChange" | "treeData"
> & {
    value?: string | string[];
    onChange?: (value: any) => void;
    fetchData: () => Promise<T[]>;
    fieldNames?: TreeFieldNames;
    multiple: boolean
};

const OptionTreeSelect = <T extends any>({
    value = [],
    onChange,
    fetchData,
    fieldNames = {},
    placeholder = "请选择",
    multiple = true,
    ...restProps
}: TreeSelectPropsType<T>) => {
    // 字段映射解构
    const idField = fieldNames.id || "id";
    const labelField = fieldNames.label || "name";
    const childrenField = fieldNames.children || "children";

    const [treeData, setTreeData] = useState<any[]>([]);

    const { run: runFetch, loading: fetchDataLoading } = useRequest(fetchData, {
        manual: true,
        onSuccess: (data) => {
            setTreeData(data || []);
        }
    });

    // 初始加载：如果有值，则需要拉取数据以显示 Label
    useEffect(() => {
        runFetch();
    }, [])

    const handleDropdownVisibleChange = (open: boolean) => {
        if (open && treeData.length === 0) {
            runFetch();
        }
    };

    /**
     * 构建父子映射关系 (用于处理选中父节点逻辑)
     */
    const { parentMap, childrenMap, availableKeys } = useMemo(() => {
        const pMap = new Map<string, string>();
        const cMap = new Map<string, string[]>();
        const aKeys = new Set<string>();

        const walk = (nodes: any[], parent?: string) => {
            nodes.forEach((node) => {
                const id = String(node[idField]);
                aKeys.add(id);
                if (parent) {
                    pMap.set(id, parent);
                    const siblings = cMap.get(parent) || [];
                    siblings.push(id);
                    cMap.set(parent, siblings);
                }
                if (node[childrenField]?.length) walk(node[childrenField], id);
            });
        };
        walk(treeData);
        return { parentMap: pMap, childrenMap: cMap, availableKeys: aKeys };
    }, [treeData, idField, childrenField]);

    // 获取所有父节点 ID
    const getAllParents = (id: string) => {
        const parents: string[] = [];
        let current = parentMap.get(id);
        while (current) {
            parents.push(current);
            current = parentMap.get(current);
        }
        return parents;
    };

    /**
     * 处理选中逻辑：当子节点选中时，自动加入所有父节点 ID
     */
    const handleChange = (checked: any) => {
        if (!multiple) {
            // 单选模式直接返回
            onChange?.(checked);
            return;
        }
        // AntD 在 treeCheckStrictly: false 时 checked 可能是简单数组
        const rawValues = Array.isArray(checked) ? checked : [];
        const finalIds = new Set<string>();

        rawValues.forEach((val: any) => {
            const id = String(val);
            finalIds.add(id);
            const parentIds = getAllParents(id);
            parentIds.forEach(pId => finalIds.add(pId));
        });

        onChange?.(Array.from(finalIds));
    };

    /**
     * 将原始数据转换为 TreeSelect 需要的格式，并处理字段展示
     */
    const mappedTreeData = useMemo(() => {
        const format = (nodes: any[]): any[] => {
            return nodes.map(node => {
                const isSelectable = node.isAllowMount ?? true
                return {
                    ...node,
                    title: node[labelField], // 展示字段
                    value: String(node[idField]), // 绑定值
                    key: String(node[idField]),
                    children: node[childrenField]?.length ? format(node[childrenField]) : [],
                    disableCheckbox: !isSelectable,
                    selectable: isSelectable
                }
            });
        };
        return format(treeData);
    }, [treeData, labelField, idField, childrenField]);

    /**
     * 计算受控显示的 value
     * AntD TreeSelect 如果开启了父子联动，checkedKeys 只需要传入叶子节点
     */
    const safeValue = useMemo(() => {
        if (value === undefined || value === null || (Array.isArray(value) && value.length === 0)) {
            return multiple ? [] : undefined;
        }
        // 如果是单选
        if (!multiple) {
            return availableKeys.has(String(value)) ? String(value) : undefined;
        }
        const valArr = Array.isArray(value) ? value : [value];
        const parentIdsInTree = new Set(childrenMap.keys());
        return valArr
            .map(v => String(v))
            .filter((id) => availableKeys.has(id) && !parentIdsInTree.has(id));
    }, [value, availableKeys, childrenMap]);

    return (
        <Spin spinning={fetchDataLoading}>
            <TreeSelect
                style={{ width: '100%' }}
                placeholder={placeholder}
                {...restProps}
                // 核心属性
                treeData={mappedTreeData}
                value={safeValue}
                onChange={handleChange}
                // 搜索过滤逻辑适配映射后的 title
                filterTreeNode={(input, treeNode) =>
                    String(treeNode.label || '').toLowerCase().includes(input.toLowerCase())
                }
                // 默认配置
                treeCheckable={multiple}
                multiple={multiple}
                treeCheckStrictly={false} // 开启 UI 上的联动
                showCheckedStrategy={TreeSelect.SHOW_ALL}
                maxTagCount={restProps.maxTagCount ?? 3}
                treeDefaultExpandAll={true}
                onOpenChange={handleDropdownVisibleChange}
            />
        </Spin>
    );
};

export default OptionTreeSelect;