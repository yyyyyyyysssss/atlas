import type { Rule } from 'antd/es/form';
import { DatePicker, Form, Input, InputNumber, Select, SelectProps } from 'antd'
import React, { useContext, useMemo } from 'react';
import { EditableContext } from './EditableContext';
import { NamePath } from 'antd/es/form/interface';
import './editable.css'
import dayjs from 'dayjs';

interface EditableCellProps {
    tableName: string
    editing: boolean
    dataIndex: string
    title: string
    rowData: any
    index: number
    children: React.ReactNode
    inputType?: 'input' | 'number' | 'select' | 'date' | 'datetime' | 'custom'
    options?: Array<{ label: string; value: any }>
    required?: boolean
    rules?: Rule[]
    onChange?: (value: any, rowIndex: number) => void
    render?: React.ReactNode | ((value: any, record: any, index: any) => React.ReactNode),
    editRender?: React.ReactNode | ((record: any) => React.ReactNode)
    rowIndex: number
}

interface EditableInputNodeProps {
    inputType?: 'input' | 'number' | 'select' | 'date' | 'datetime' | 'custom'
    options?: SelectProps['options']
    value?: any
    onChange: (value: any) => void
    editRender?: ((props: { value: any; onChange: (val: any) => void }, record: any) => React.ReactNode) | React.ReactNode
    record: any
}

const EditableInputNode: React.FC<EditableInputNodeProps> = React.memo(({
    inputType,
    options,
    value,
    onChange,
    editRender,
    record
}) => {
    switch (inputType) {
        case 'custom':
            return typeof editRender === 'function' ? editRender({ value, onChange }, record) : editRender
        case 'number':
            return <InputNumber style={{ width: '100%' }} value={value} onChange={onChange} />
        case 'date':
            return <DatePicker style={{ width: '100%' }} value={value ? dayjs(value) : null} onChange={(_, str) => onChange(str)} />
        case 'datetime':
            return <DatePicker showTime style={{ width: '100%' }} value={value ? dayjs(value) : null} onChange={(_, str) => onChange(str)} />
        case 'select':
            return <Select style={{ width: '100%' }} options={options} value={value} onChange={onChange} allowClear />
        case 'input':
        default:
            return <Input style={{ width: '100%' }} value={value} onChange={(e) => onChange(e.target.value)} />
    }
})

const EditableCell: React.FC<EditableCellProps> = ({
    tableName,
    editing,
    dataIndex,
    title,
    rowData,
    index,
    children,
    inputType = 'input',
    options = [],
    required = false,
    rules,
    onChange,
    render,
    editRender,
    rowIndex,
    ...restProps
}) => {

    const form = Form.useFormInstance()

    const { rowOnChange } = useContext(EditableContext) || {}

    const mergedRules = [
        ...(required ? [{ required: true, message: `${title}不能为空` }] : []),
        ...(rules || []),
    ]

    const handleChange = (val: any) => {
        const newRowData = { ...rowData, [dataIndex]: val }
        // 更新表单中的字段值，确保同步
        form.setFieldsValue({
            [tableName]: form.getFieldValue(tableName).map((row: any, index: number) =>
                index === rowIndex ? { ...row, [dataIndex]: val } : row
            ),
        })
        onChange?.(val, rowIndex)
        rowOnChange?.(newRowData, rowIndex)
    }

    return (
        <td {...restProps}>
            {editing
                ?
                (
                    <Form.Item
                        className="editable-cell-form-item"
                        name={[rowIndex, dataIndex] as NamePath}
                        style={{ margin: 0 }}
                        rules={mergedRules}
                        labelCol={{ span: 0 }}
                        wrapperCol={{ span: 24 }}
                    >
                        <EditableInputNode
                            inputType={inputType}
                            options={options}
                            value={rowData[dataIndex]}
                            onChange={handleChange}
                            editRender={editRender}
                            record={rowData}
                        />
                    </Form.Item>
                )
                :
                (
                    render
                        ? typeof render === 'function'
                            ? render(rowData[dataIndex], rowData, index) // 如果 render 是函数，调用它
                            : render // 如果 render 是 ReactNode 类型，直接渲染
                        : (rowData?.[dataIndex] !== undefined ? rowData[dataIndex]?.toString() : children)
                )
            }
        </td>
    )
}

export default EditableCell