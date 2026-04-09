import { useSortable } from '@dnd-kit/sortable'
import './index.css'
import { Checkbox, CheckboxChangeEvent, Dropdown, Flex, List, Table, Tooltip, Typography } from 'antd'
import { RotateCw, Settings, ArrowUpToLine, GripVertical, ArrowDownToLine, MoveVertical, Printer, Download } from 'lucide-react'
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { CSS } from '@dnd-kit/utilities'
import type { TableProps, ColumnsType } from 'antd/es/table'
import { useTranslation } from 'react-i18next';
import {
    DndContext,
    closestCenter,
    KeyboardSensor,
    PointerSensor,
    useSensor,
    useSensors,
    DragEndEvent,
} from '@dnd-kit/core'
import {
    arrayMove,
    SortableContext,
    sortableKeyboardCoordinates,
    verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { useReactToPrint } from 'react-to-print'
import { downloadFileBySource } from '../../utils/Download'
import html2canvas from 'html2canvas'
import Loading from '../loading'
import { Resizable } from 'react-resizable';


interface FieldNames {
    list?: string
    pageNum?: string
    pageSize?: string
    total?: string
}

interface SmartTableProps<T = any> extends TableProps<T> {
    columns: ColumnsType<T>
    headerExtra?: React.ReactNode
    storageKey?: string
    fetchData: (queryParam: any) => Promise<any>
    queryParam: any
    setQueryParam: (param: any) => void
    fieldNames?: FieldNames
    autoFetch?: boolean
    rowKey: string
    transformData?: (data: T[]) => T[]
    onDataChange?: (data: T[]) => T[]
}

interface SortableItemProps {
    item: any
    index: number
    tableColumns: any[]
    unfixedColumns: any[]
    onToggleColumn: (e: CheckboxChangeEvent, key: string) => void
    onFixedHeader: (key: string) => void
    onFixedFooter: (key: string) => void
}

const reorderColumnsForFixed = (columns: any[]) => {
    const left = []
    const middle = []
    const right = []

    for (const col of columns) {
        if (col.fixed === 'left') left.push(col)
        else if (col.fixed === 'right') right.push(col)
        else middle.push(col)
    }

    return [...left, ...middle, ...right]
}

const SortableItem: React.FC<SortableItemProps> = ({
    item,
    index,
    tableColumns,
    unfixedColumns,
    onToggleColumn,
    onFixedHeader,
    onFixedFooter
}) => {

    const { t } = useTranslation()

    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
    } = useSortable({ id: item.key })
    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
        padding: '4px 0'
    }
    const showUnfixedTitle = index === 0 && tableColumns.length !== unfixedColumns.length
    return (
        <Flex vertical>
            {showUnfixedTitle && (
                <Typography.Text type="secondary" style={{ fontSize: 12, marginLeft: '25px' }} >{t('不固定')}</Typography.Text>
            )}
            <List.Item ref={setNodeRef} style={style} {...attributes} className="hoverable-list-item">
                <Flex
                    flex={1}
                    justify='space-between'
                    align='center'
                >
                    <Flex gap={25} justify='center' align='center'>
                        <span {...listeners}>
                            <GripVertical style={{ cursor: 'grab' }} color='var(--ant-color-text-disabled)' size={16} />
                        </span>
                        <Flex flex={1}>
                            <Checkbox
                                style={{ width: '100%' }}
                                onChange={(e) => onToggleColumn(e, item.key)}
                                checked={item.visible !== false}
                            >
                                <Typography.Text
                                    className='typography-text-checkbox-title'
                                    ellipsis={{ tooltip: true }}
                                >
                                    {item.title}
                                </Typography.Text>
                            </Checkbox>
                        </Flex>
                    </Flex>
                    <Flex className='actions' gap={6}>
                        <Tooltip title={t('固定在列首')}>
                            <Typography.Link onClick={() => onFixedHeader(item.key)}>
                                <ArrowUpToLine size={16} />
                            </Typography.Link>
                        </Tooltip>
                        <Tooltip title={t('固定在列尾')}>
                            <Typography.Link onClick={() => onFixedFooter(item.key)}>
                                <ArrowDownToLine size={16} />
                            </Typography.Link>
                        </Tooltip>
                    </Flex>
                </Flex>
            </List.Item>
        </Flex>
    )
}

const SmartTable = <T extends any>({
    columns,
    headerExtra,
    storageKey,
    fetchData,
    loading,
    queryParam,
    setQueryParam,
    fieldNames,
    autoFetch = true,
    rowKey,
    transformData,
    onDataChange,
    ...rest
}: SmartTableProps<T>) => {

    const location = useLocation()

    const { t } = useTranslation()

    const isDev = import.meta.env.MODE === 'dev'

    const contentRef = useRef<HTMLDivElement>(null)

    const [isPrinting, setIsPrinting] = useState(false)

    const [exporting, setExporting] = useState(false)

    const handlePrint = useReactToPrint({
        contentRef: contentRef,
        onBeforePrint: () => {
            setIsPrinting(true)
            return Promise.resolve()
        },
        onAfterPrint: () => {
            // 打印后：关闭打印模式，恢复操作列
            setIsPrinting(false)
        },
        documentTitle: t('数据报表'),
    })

    const STORAGE_KEY = storageKey || `smart_table_${location.pathname}`

    const [tableColumns, setTableColumns] = useState<any[]>([])

    const {
        list: listField = 'list',
        pageNum: pageNumField = 'pageNum',
        pageSize: pageSizeField = 'pageSize',
        total: totalField = 'total'
    } = fieldNames || {}

    const [data, setData] = useState<any>({})

    const isFirstRender = useRef(true)

    useEffect(() => {
        if (isFirstRender.current) {
            isFirstRender.current = false
            if (!autoFetch) return // 首次渲染 + autoFetch=false → 跳过
        }
        fetchData(queryParam).then(rawData => {
            const processed = transformData ? transformData(rawData) : rawData;
            setData(processed)
            onDataChange?.(processed)
        })
    }, [queryParam])

    const handleRefresh = () => {
        fetchData(queryParam)
    }

    useEffect(() => {
        const storageColums = localStorage.getItem(STORAGE_KEY)
        if (storageColums && !isDev) {
            const parsedColums = JSON.parse(storageColums)
            const merged = parsedColums.map((saved: any) => {
                const col: any = columns.find((c) => c.key === (saved.key || saved.dataIndex))
                return {
                    ...col, // 最新配置
                    ...saved, // 用户偏好
                    key: col?.key || col?.dataIndex,
                }
            })
            setTableColumns(merged)
        } else {
            const updatedColumns = columns.map((col: any) => ({
                ...col,
                key: col.key || col.dataIndex, // 如果没有 key，则使用 dataIndex 作为 key
            }))
            setTableColumns(updatedColumns)
        }
    }, [columns])

    useEffect(() => {
        if (!isDev && tableColumns.length > 0) {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(tableColumns.map(({ key, visible, fixed }) => ({ key, visible, fixed }))))
        }
    }, [tableColumns])

    const sensors = useSensors(
        useSensor(PointerSensor),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        })
    )

    const total = tableColumns.length
    const checkedCount = tableColumns.filter(col => col.visible != false).length

    const checkAll = useMemo(() => checkedCount === total, [checkedCount, total])
    const indeterminate = useMemo(() => checkedCount > 0 && checkedCount < total, [checkedCount, total])

    const visibleColumns = useMemo(() => {
        return tableColumns
            .filter(col => {
                if (col.visible === false) {
                    return false
                }
                if (isPrinting) {
                    const isActionColumn = col.title === t('操作') || col.className?.includes('no-print')
                    return !isActionColumn
                }
                return true
            })
    }, [tableColumns, t, isPrinting])

    const handleResize = useCallback((key: string) => (e: React.SyntheticEvent, { size }: any) => {
        setTableColumns((prev) => {
            return prev.map((col) => {
                if (col.key === key) {
                    return { ...col, width: size.width }
                }
                return col
            })
        })
    }, [])

    const mergedColumns = useMemo(() => {
        return visibleColumns.map((col) => {
            const isAction = col.title === t('操作') || col.key === 'operation'
            // 如果是操作列，给它一个固定宽度，不注入拖拽逻辑
            if (isAction) {
                return {
                    ...col,
                    width: col.width || 180, // 必须给数字，防止被挤压
                }
            }
            // 如果列没有配置初始宽度，为了能拖拽，给个默认值
            const rawWidth = col.width;
            const parsedWidth = typeof rawWidth === 'number' ? rawWidth : parseInt(String(rawWidth)) || 150;

            return {
                ...col,
                width: parsedWidth,
                onHeaderCell: (column: any) => ({
                    width: column.width,
                }),
            }
        })
    }, [visibleColumns, handleResize])

    const handleCheckAllChange = (e: CheckboxChangeEvent) => {
        setTableColumns(prev => prev.map(col => ({ ...col, visible: e.target.checked })))
    }

    const handleToggleColumn = (e: CheckboxChangeEvent, key: string) => {
        const checked = e.target.checked
        setTableColumns(prev => prev.map(col => col.key === key ? { ...col, visible: checked } : col))
    }

    const handleFixedHeader = (key: string) => {
        setTableColumns(prev => {
            const updated = prev.map(col => col.key === key ? { ...col, fixed: 'left' } : col)
            return reorderColumnsForFixed(updated)
        })
    }

    const handleNotFixed = (key: string) => {
        setTableColumns(prev => {
            const updated = prev.map(col => col.key === key ? { ...col, fixed: undefined } : col)
            return reorderColumnsForFixed(updated)
        })
    }

    const handleFixedFooter = (key: string) => {
        setTableColumns(prev => {
            const updated = prev.map(col => col.key === key ? { ...col, fixed: 'right' } : col)
            return reorderColumnsForFixed(updated)
        })
    }

    const renderHeaderItem = useMemo(() => {
        const headerItemTableColumns = tableColumns.filter(item => item.fixed === 'left') || []
        if (headerItemTableColumns.length === 0) {
            return <></>
        }
        return (
            <Flex style={{ marginLeft: '25px' }} vertical>
                <Typography.Text type="secondary" style={{ fontSize: 12 }} >{t('固定在左侧')}</Typography.Text>
                {
                    headerItemTableColumns
                        .map(item => (
                            <List.Item key={item.key} className="hoverable-list-item" style={{ padding: '4px 0' }}>
                                <Flex
                                    flex={1}
                                    justify='space-between'
                                    align='center'
                                >
                                    <Flex justify='center' align='center'>
                                        <Checkbox style={{ width: '100%' }} onChange={(e) => handleToggleColumn(e, item.key)} checked={item.visible != false}>
                                            <Typography.Text
                                                className='typography-text-checkbox-title'
                                                ellipsis={{ tooltip: true }}
                                            >
                                                {item.title}
                                            </Typography.Text>
                                        </Checkbox>
                                    </Flex>
                                    <Flex className='actions' gap={6}>
                                        <Tooltip title={t('不固定')}>
                                            <Typography.Link onClick={() => handleNotFixed(item.key)}>
                                                <MoveVertical size={16} />
                                            </Typography.Link>
                                        </Tooltip>
                                        <Tooltip title={t('固定在列尾')}>
                                            <Typography.Link onClick={() => handleFixedFooter(item.key)}>
                                                <ArrowDownToLine size={16} />
                                            </Typography.Link>
                                        </Tooltip>
                                    </Flex>
                                </Flex>
                            </List.Item>
                        ))
                }
            </Flex>
        )
    }, [tableColumns, handleNotFixed, handleFixedFooter])

    const unfixedColumns = useMemo(() => {
        return tableColumns.filter(item => item.fixed !== 'left' && item.fixed !== 'right' && !item.hidden)
    }, [tableColumns])

    const renderFooterItem = useMemo(() => {
        const footerItemTableColumns = tableColumns.filter(item => item.fixed === 'right') || []
        if (footerItemTableColumns.length === 0) {
            return <></>
        }
        return (
            <Flex style={{ marginLeft: '25px' }} vertical>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>{t('固定在右侧')}</Typography.Text>
                {
                    footerItemTableColumns
                        .map(item => (
                            <List.Item key={item.key} className="hoverable-list-item" style={{ padding: '4px 0' }}>
                                <Flex
                                    flex={1}
                                    justify='space-between'
                                    align='center'
                                >
                                    <Flex justify='center' align='center'>
                                        <Checkbox style={{ width: '100%' }} onChange={(e) => handleToggleColumn(e, item.key)} checked={item.visible != false}>
                                            <Typography.Text
                                                className='typography-text-checkbox-title'
                                                ellipsis={{ tooltip: true }}
                                            >
                                                {item.title}
                                            </Typography.Text>
                                        </Checkbox>
                                    </Flex>
                                    <Flex className='actions' gap={6}>
                                        <Tooltip title={t('不固定')}>
                                            <Typography.Link onClick={() => handleNotFixed(item.key)}>
                                                <MoveVertical size={16} />
                                            </Typography.Link>
                                        </Tooltip>
                                        <Tooltip title={t('固定在列首')}>
                                            <Typography.Link onClick={() => handleFixedHeader(item.key)}>
                                                <ArrowUpToLine size={16} />
                                            </Typography.Link>
                                        </Tooltip>
                                    </Flex>
                                </Flex>
                            </List.Item>
                        ))
                }
            </Flex>
        )
    }, [tableColumns])


    const handleDragEnd = (event: DragEndEvent) => {
        const { active, over } = event
        if (!over || active.id === over.id) return

        setTableColumns((items) => {
            const oldIndex = items.findIndex((item) => item.key === active.id)
            const newIndex = items.findIndex((item) => item.key === over.id)

            if (oldIndex === -1 || newIndex === -1) return items // 防御性判断

            return arrayMove(items, oldIndex, newIndex)
        })
    }

    const handleExportCSV = () => {
        const dataSource = data?.[listField] || []
        if (dataSource.length === 0) {
            return
        }
        setExporting(true)
        try {
            // 提取表头
            const headers = visibleColumns
                .filter(col => col.title && col.title !== t('操作') && col.dataIndex)
                .map(col => col.title as string)
            // 构建 CSV 内容
            const csvRows = [
                headers.join(','),
                ...dataSource.map((record: any, index: number) => {
                    return visibleColumns
                        .filter(col => col.title && col.title !== t('操作') && col.dataIndex)
                        .map(col => {
                            const rawValue = record[col.dataIndex as string] ?? ''
                            let value = rawValue
                            if (col.render) {
                                const rendered = col.render(rawValue, record, index);

                                if (React.isValidElement(rendered)) {
                                    // 关键点：如果 children 是字符串或数字，才取它；否则放弃赋值，保持原始值
                                    const child = (rendered.props as any).children;
                                    if (typeof child === 'string' || typeof child === 'number') {
                                        value = child;
                                    }
                                } else if (rendered !== null && typeof rendered === 'object') {
                                    // 处理 antd 的 { children: ReactNode, props: {} }
                                    const child = (rendered as any).children;
                                    if (typeof child === 'string' || typeof child === 'number') {
                                        value = child;
                                    }
                                } else if (typeof rendered === 'string' || typeof rendered === 'number') {
                                    // 只有纯文本或数字才覆盖原始值
                                    value = rendered;
                                }
                            }
                            if (typeof value === 'object' && value !== null) {
                                value = rawValue;
                            }
                            const finalValue = value ?? ''
                            // 处理数据中的逗号、换行，防止破坏 CSV 结构
                            const cellContent = `\t${String(finalValue).replace(/"/g, '""')}`
                            return `"${cellContent}"`
                        })
                        .join(',')
                })
            ]
            const csvString = csvRows.join('\n')
            const BOM = '\uFEFF'
            const blob = new Blob([BOM + csvString], { type: 'text/csv;charset=utf-8;' })
            const finalFileName = `${storageKey || 'export'}_${new Date().getTime()}.csv`;
            downloadFileBySource(blob, finalFileName)
        } finally {
            setExporting(false)
        }
    }

    const handleExportImage = async () => {
        if (!contentRef.current) return
        setExporting(true)
        const tableElement = contentRef.current
        // 延迟执行，给 Spin 渲染的时间
        setTimeout(async () => {
            try {
                const canvas = await html2canvas(tableElement, {
                    useCORS: true,
                    scale: 2, // 提高清晰度
                    backgroundColor: null,
                    onclone: (clonedDoc) => {

                    },
                    ignoreElements: (element) => {
                        // 过滤掉不需要出现在截图中的元素
                        return element.classList.contains('ant-pagination');
                    }
                });

                const dataUrl = canvas.toDataURL("image/png");
                const finalFileName = `${STORAGE_KEY}_${new Date().getTime()}.png`
                downloadFileBySource(dataUrl, finalFileName)
            } catch (error) {
                console.error('图片导出失败', error)
            } finally {
                setExporting(false)
            }
        }, 200)

    }

    return (
        <Flex
            gap={10}
            vertical
        >
            <Flex
                justify='space-between'
                align='center'
            >
                {headerExtra !== undefined && headerExtra !== null ? headerExtra : <div />}
                <Flex
                    style={{ marginRight: 8 }}
                    gap={10}
                >
                    <Tooltip title={t('刷新')}>
                        <Typography.Text onClick={handleRefresh} className='typography-text-icon'>
                            <RotateCw size={18} />
                        </Typography.Text>
                    </Tooltip>
                    <Dropdown
                        trigger={['click']}
                        popupRender={() => (
                            <Flex gap={10} className="ant-dropdown-menu" style={{ width: '220px', padding: 10 }} vertical>
                                <Flex justify='space-between'>
                                    <Checkbox indeterminate={indeterminate} onChange={handleCheckAllChange} checked={checkAll}>
                                        {t('列展示')}
                                    </Checkbox>
                                    <Typography.Link onClick={() => setTableColumns(columns)}>
                                        {t('重置')}
                                    </Typography.Link>
                                </Flex>
                                <Flex vertical>
                                    {renderHeaderItem}
                                    <DndContext
                                        sensors={sensors}
                                        collisionDetection={closestCenter}
                                        onDragEnd={handleDragEnd}
                                    >
                                        <SortableContext
                                            items={unfixedColumns.map((item) => item.key)}
                                            strategy={verticalListSortingStrategy}
                                        >
                                            <List
                                                split={false}
                                                style={{
                                                    maxHeight: '400px',
                                                    overflowY: 'auto',
                                                    overflowX: 'hidden'
                                                }}
                                                dataSource={unfixedColumns}
                                                renderItem={(item, index) => (
                                                    <SortableItem
                                                        key={item.key}
                                                        item={item}
                                                        index={index}
                                                        tableColumns={tableColumns}
                                                        unfixedColumns={unfixedColumns}
                                                        onToggleColumn={handleToggleColumn}
                                                        onFixedHeader={handleFixedHeader}
                                                        onFixedFooter={handleFixedFooter}
                                                    />
                                                )}
                                            />
                                        </SortableContext>
                                    </DndContext>
                                    {renderFooterItem}
                                </Flex>
                            </Flex>
                        )}
                    >
                        <Tooltip title={t('列设置')}>
                            <Typography.Text className='typography-text-icon'>
                                <Settings size={18} />
                            </Typography.Text>
                        </Tooltip>
                    </Dropdown>
                    <Tooltip title={t('导出当前页')}>
                        <Dropdown
                            trigger={['click']}
                            disabled={exporting}
                            menu={{
                                items: [
                                    {
                                        key: 'export_csv',
                                        label: '.csv',
                                        onClick: handleExportCSV,
                                        disabled: exporting, // 也可以双重保险，单独禁用 item
                                    },
                                    {
                                        key: 'export_png',
                                        label: '.png',
                                        onClick: handleExportImage,
                                        disabled: exporting,
                                    }
                                ]
                            }}
                        >
                            <Loading spinning={exporting} size='small'>
                                <Typography.Text
                                    className='typography-text-icon'
                                >
                                    <Download size={18} />
                                </Typography.Text>
                            </Loading>
                        </Dropdown>
                    </Tooltip>
                    <Tooltip title={t('打印当前页')}>
                        <Typography.Text
                            onClick={() => handlePrint()}
                            className='typography-text-icon'
                            style={{ cursor: 'pointer', display: 'flex' }}
                        >
                            <Printer size={18} />
                        </Typography.Text>
                    </Tooltip>
                </Flex>
            </Flex>
            <div ref={contentRef} className="smart-table-printable">
                <Table
                    className='w-full'
                    columns={visibleColumns}
                    loading={loading}
                    scroll={data?.[listField]?.length > 10 ? { y: 600, x: 'max-content' } : { x: 'max-content' }}
                    dataSource={data?.[listField] || []}
                    rowKey={rowKey || 'id'}
                    pagination={{
                        current: data?.[pageNumField],
                        pageSize: data?.[pageSizeField],
                        total: data?.[totalField],
                        showQuickJumper: true,
                        showSizeChanger: true,
                        pageSizeOptions: ['10', '20', '50', '100'],
                        showTotal: total => t('共 {{total}} 条', { total }),
                        onChange: (pageNum, pageSize) => {
                            const newQueryParam = { ...queryParam, [pageNumField]: pageNum, [pageSizeField]: pageSize }
                            setQueryParam(newQueryParam)
                        }
                    }}
                    {...rest}
                />
            </div>
        </Flex>
    )
}

export default SmartTable