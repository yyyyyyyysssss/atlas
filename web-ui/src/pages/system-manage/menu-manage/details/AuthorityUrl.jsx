import { Flex, Form, Select, Tag } from 'antd'
import { RequestMethod } from '../../../../enums/common'
import { useEffect } from 'react'
import EditableTable from '../../../../components/smart-table/EditableTable'
import { useTranslation } from 'react-i18next';

const requestMethodOptions = Object.entries(RequestMethod).map(([key, value]) => ({
    label: key,
    value: value,
}))

const AuthorityUrl = ({ authorityId, authorityUrls, onChange, loading }) => {

    const [form] = Form.useForm()

    const { t } = useTranslation()

    useEffect(() => {
        if (authorityUrls) {
            form.setFieldsValue({
                urls: authorityUrls
            })
        }
    }, [authorityUrls])


    const handleSave = async (_, rowIndex) => {
        const formValues = await form.validateFields()
        const { urls } = formValues
        await onChange(urls)

    }

    const handleDelete = async (_, rowIndex) => {
        const formValues = await form.validateFields()
        const urls = [...formValues.urls]
        urls.splice(rowIndex, 1)
        await onChange(urls)

    }

    const columns = [
        {
            key: 'method',
            title: '请求方法',
            dataIndex: 'method',
            align: 'center',
            editable: true,
            inputType: 'custom',
            required: true,
            onChange: (val, rowIndex) => {

            },
            editRender: ({ value, onChange }) => {
                return <Select style={{ width: '100%' }} options={requestMethodOptions} value={value} onChange={onChange} />
            },
            render: (_, { method }) => {
                switch (method?.toUpperCase()) {
                    case RequestMethod.GET:
                        return <Tag color="green">GET</Tag>
                    case RequestMethod.POST:
                        return <Tag color="blue">POST</Tag>
                    case RequestMethod.PUT:
                        return <Tag color="orange">PUT</Tag>
                    case RequestMethod.PATCH:
                        return <Tag color="yellow">PATCH</Tag>
                    case RequestMethod.DELETE:
                        return <Tag color="red">DELETE</Tag>
                    case RequestMethod.ALL:
                        return <Tag color="purple">ALL</Tag>
                    default:
                        return <Tag color="gray">未知</Tag>
                }
            }
        },
        {
            key: 'url',
            title: '请求路径',
            dataIndex: 'url',
            align: 'center',
            editable: true,
            required: true,
        },
    ]


    return (
        <Form form={form} component={false}>
            <Flex gap={8} vertical>
                <Form.List
                    name="urls"
                    noStyle
                >
                    {(fields, { add, remove }) => (
                        <EditableTable
                            className='menu-authority'
                            columns={columns}
                            name='urls'
                            mode='single-edit'
                            fields={fields}
                            editPermission={'system:menu:write'}
                            deletePermission={'system:menu:delete'}
                            add={add}
                            remove={remove}
                            onSave={handleSave}
                            onDelete={handleDelete}
                        />
                    )}
                </Form.List>
            </Flex>
        </Form>
    )

}

export default AuthorityUrl