import React from 'react'
import { EditableContext } from './EditableContext'
import './editable.css'

interface EditableRowProps {
    record: any
    rowOnChange?: (rowData: any) => void
    children: React.ReactNode
}

const EditableRow: React.FC<EditableRowProps> = ({
    record = {},
    rowOnChange,
    children,
    ...props
}) => {

    return (
        <EditableContext.Provider value={{ rowOnChange }}>
            <tr {...props}>{children}</tr>
        </EditableContext.Provider>
    )
}

export default EditableRow