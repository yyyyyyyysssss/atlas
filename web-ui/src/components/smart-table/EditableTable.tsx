import { Button, ConfigProvider, Flex, Form, Popconfirm, Space, Table, Typography } from "antd"
import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import EditableRow from "./EditableRow"
import EditableCell from "./EditableCell"
import React from "react"
import './editable.css'
import IdGen from "../../utils/IdGen"
import { getMessageApi } from "../../utils/MessageUtil"
import HasPermission from '../../components/HasPermission';
import Loading from "../loading"
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { OperationMode } from '../../enums/common'


type OperationModeType = typeof OperationMode[keyof typeof OperationMode]['value']

interface EditableTableProps {
    columns: any[]
    name: string
    rowKey?: string
    fields: any[]
    mode: 'single-edit' | 'multi-add'
    operationMode: OperationModeType
    addPermission?: string | string[]
    editPermission?: string | string[]
    deletePermission?: string | string[]
    errors: any,
    add: (defaultValue?: any, insertIndex?: number) => void
    remove: (index: number) => void
    onSave?: (rowData: any, rowIndex: number) => Promise<any>
    onDelete?: (rowData: any, rowIndex: number) => Promise<any>
    loading: boolean
    renderExtraActions?: (record: any, rowIndex: number, isAnyRowEditing: boolean) => React.ReactNode
}

const EditableTable: React.FC<EditableTableProps> = ({
    columns,
    name,
    rowKey = 'id',
    fields,
    mode = 'multi-add',
    operationMode = OperationMode.ADD.value,
    editPermission,
    addPermission = editPermission,
    deletePermission,
    errors,
    add,
    remove,
    onSave,
    onDelete,
    loading = false,
    renderExtraActions,
    ...props
}) => {

    const { t } = useTranslation()

    const form = Form.useFormInstance()

    const { componentDisabled } = ConfigProvider.useConfig()

    const [editingKey, setEditingKey] = useState<string | null>(null)
    const [pendingAdd, setPendingAdd] = useState<any>({
        key: null,
        flag: false
    })

    const [loadingMap, setLoadingMap] = useState<any>({})

    const editRowDataRef = useRef<any>(null)

    const tableData = useMemo(() => {
        const listValues = form.getFieldValue(name) || []
        return fields.map((field, index) => {
            const recordValue = listValues[index] || {}
            const keyValue = recordValue[rowKey] || field.key
            return {
                ...recordValue,
                key: keyValue,
                [rowKey]: keyValue,
            }
        })
    }, [fields, form, name, rowKey])

    const isEditing = useCallback((rowEditingKey: any) => {

        return mode === 'multi-add' ? true : rowEditingKey === editingKey
    }, [mode, rowKey, editingKey])

    const addRow = useCallback((rowData: any) => {
        const newRowData = { ...rowData, type: 'add' }
        add(newRowData)
    }, [add])

    useEffect(() => {
        if (pendingAdd.flag === true && editingKey && pendingAdd.key) {
            const newRowData = { [rowKey]: pendingAdd.key }
            addRow(newRowData)
            setPendingAdd({
                key: null,
                flag: false
            })
        }
    }, [editingKey, pendingAdd, rowKey])

    const handleAdd = useCallback(async () => {
        const newKey = IdGen.nextId()
        if (mode === 'single-edit') {
            if (editingKey !== null) {
                getMessageApi().warning(t('请先保存或取消当前正在编辑的行'))
                return
            }
            setEditingKey(newKey)
            setPendingAdd({
                key: newKey,
                flag: true
            })
        } else {
            const newRowData = { [rowKey]: newKey }
            addRow(newRowData)
        }
    }, [mode, editingKey, addRow, rowKey])


    const handleSave = useCallback(async (name: any, rowIndex: number) => {
        const rowData = form.getFieldValue([name, rowIndex])
        setLoadingMap((prev: any) => ({ ...prev, [rowIndex]: true }))
        try {
            await rowValidate(rowIndex)
            await onSave?.(rowData, rowIndex)
            setEditingKey(null)
            editRowDataRef.current = null
        } finally {
            setLoadingMap((prev: any) => ({ ...prev, [rowIndex]: false }))
        }

    }, [form, onSave])

    const handleEdit = useCallback((name: any, rowIndex: number) => {
        const rowData = form.getFieldValue([name, rowIndex])
        editRowDataRef.current = rowData
        setEditingKey(rowData[rowKey])
        form.setFieldsValue({
            [name]: form.getFieldValue(name).map((row: any, index: number) =>
                index === rowIndex ? { ...row, ...rowData, type: 'edit' } : row
            ),
        })
    }, [form, name, rowKey])

    const handleCancel = useCallback((name: any, rowIndex: number) => {
        const rowData = form.getFieldValue([name, rowIndex])
        setEditingKey(null)
        if (rowData.type && rowData.type === 'add') {
            remove(rowIndex)
        } else {
            const oldRowData = editRowDataRef.current || {}
            form.setFieldsValue({
                [name]: form.getFieldValue(name).map((row: any, index: number) =>
                    index === rowIndex ? { ...row, ...oldRowData } : row
                ),
            })
        }
        editRowDataRef.current = null
    }, [form, name, rowKey, remove])


    const handleDelete = useCallback(async (name: any, rowIndex: number) => {
        const rowData = form.getFieldValue([name, rowIndex])
        await onDelete?.(rowData, rowIndex)
        remove(rowIndex)
        setEditingKey(null)
    }, [onDelete, remove, form])

    const rowValidate = useCallback((rowIndex: number) => {
        const fields = columns
            .filter(col => col.dataIndex)
            .map(col => [name, rowIndex, col.dataIndex])
        return form.validateFields(fields);
    }, [columns, form, name])

    const handleCopy = useCallback((name: any, rowIndex: number) => {
        const rowData = form.getFieldValue([name, rowIndex])
        const copyRow = { ...rowData, [rowKey]: IdGen.nextId() } // 确保新行有唯一的 rowKey
        add(copyRow) // 添加新行
    }, [add, rowKey, form])

    const mergedColumns = useMemo(() => {

        return [
            ...columns.map((col) => ({
                ...col,
                onCell: (record: any, rowIndex: number) => {
                    const rowData = form.getFieldValue([name, rowIndex])
                    return {
                        tableName: name,
                        rowData: rowData,
                        dataIndex: col.dataIndex,
                        title: col.title,
                        inputType: col.inputType,
                        options: col.options,
                        required: col.required,
                        rules: col.rules,
                        render: col.render,
                        editRender: col.editRender,
                        editing: isEditing(record[rowKey]) && col.editable && col.editable !== false && componentDisabled === false,
                        onChange: col.onChange,
                        rowIndex
                    }
                },
            })),
            {
                title: '操作',
                hidden: operationMode != null && operationMode === OperationMode.VIEW.value,
                dataIndex: 'operation',
                width: 100,
                align: 'center',
                fixed: 'right',
                render: (_: any, record: any, rowIndex: number) => {
                    if (mode === 'multi-add') {
                        return (
                            componentDisabled === false && (
                                <Flex gap={8} justify='center' align='center'>
                                    <Typography.Link
                                        onClick={() => handleCopy(name, rowIndex)}
                                        style={{ whiteSpace: 'nowrap' }}
                                    >
                                        {t('复制')}
                                    </Typography.Link>
                                    <Popconfirm title={t('确定删除')} onConfirm={() => handleDelete(name, rowIndex)}>
                                        <Typography.Link style={{ whiteSpace: 'nowrap' }}>{t('删除')}</Typography.Link>
                                    </Popconfirm>
                                </Flex>
                            )
                        )
                    } else {
                        const editable = isEditing(record[rowKey])
                        return editable ?
                            (
                                componentDisabled === false && (
                                    <Flex gap={8} justify='center' align='center'>
                                        <HasPermission hasPermissions={editPermission}>
                                            {!!loadingMap[rowIndex] ? (
                                                <Loading />
                                            ) : (
                                                <Typography.Link style={{ whiteSpace: 'nowrap' }} onClick={() => handleSave(name, rowIndex)}>
                                                    {t('保存')}
                                                </Typography.Link>
                                            )}

                                            <Typography.Link style={{ whiteSpace: 'nowrap' }} onClick={() => handleCancel(name, rowIndex)}>
                                                {t('取消')}
                                            </Typography.Link>
                                        </HasPermission>
                                    </Flex >
                                )
                            )
                            :
                            (
                                componentDisabled === false && (
                                    <Flex gap={8} justify='center' align='center'>
                                        <HasPermission hasPermissions={editPermission}>
                                            <Typography.Link style={{ whiteSpace: 'nowrap' }} disabled={editingKey !== null} onClick={() => handleEdit(name, rowIndex)}>
                                                {t('编辑')}
                                            </Typography.Link>
                                        </HasPermission>
                                        <HasPermission hasPermissions={deletePermission}>
                                            <Popconfirm disabled={editingKey !== null} okText={t('确定')} cancelText={t('取消')} title={t('确定删除')} onConfirm={() => handleDelete(name, rowIndex)}>
                                                <Typography.Link style={{ whiteSpace: 'nowrap' }} disabled={editingKey !== null}>
                                                    {t('删除')}
                                                </Typography.Link>
                                            </Popconfirm>
                                        </HasPermission>
                                        {renderExtraActions && renderExtraActions(record, rowIndex, editingKey !== null)}
                                    </Flex>
                                )
                            )
                    }

                },
            },
        ]
    }, [columns, form, name, handleCopy, remove, handleDelete, handleSave, handleEdit, handleCancel, editingKey, rowKey, componentDisabled, operationMode])

    return (
        <Flex gap={8} vertical>
            <Table
                className="edit-table"
                components={{
                    body: {
                        row: EditableRow,
                        cell: EditableCell,
                    },
                }}
                dataSource={tableData}
                columns={mergedColumns}
                rowKey={rowKey}
                pagination={false}
                scroll={{ x: 'max-content', y: 280 }}
                footer={() => (
                    operationMode && operationMode !== OperationMode.VIEW.value && (
                        <HasPermission hasPermissions={addPermission}>
                            <Button onClick={handleAdd} type="dashed" style={{ width: '100%' }}><PlusOutlined />{t('新增一行')}</Button>
                        </HasPermission>
                    )
                )}
                loading={loading}
                {...props}
            />
            <Form.Item>
                <Form.ErrorList
                    errors={errors}
                />
            </Form.Item>
        </Flex>
    )
}

export default EditableTable