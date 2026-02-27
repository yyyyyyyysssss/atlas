import React from 'react'

interface EditableContextProps {
  rowOnChange?: (rowData: any, rowIndex: number) => void
  rowIndex?: number
}

export const EditableContext = React.createContext<EditableContextProps | null>(null)