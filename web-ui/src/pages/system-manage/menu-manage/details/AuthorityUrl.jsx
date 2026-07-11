import { Flex, Form, Select, Tag } from 'antd'
import { RequestMethod } from '../../../../enums/common'
import { useEffect } from 'react'
import EditableTable from '../../../../components/smart-table/EditableTable'
import { useTranslation } from 'react-i18next';
import { deleteAuthorityUrl, getAuthorityUrl, saveAuthorityUrl } from '../../../../services/SystemService';
import { useRequest } from 'ahooks';

const requestMethodOptions = Object.entries(RequestMethod).map(([key, value]) => ({
    label: key,
    value: value,
}))

const AuthorityUrl = ({ authorityId, loading }) => {

    const [form] = Form.useForm()

    const { t } = useTranslation()

    const { loading: getAuthorityUrlLoading, refresh: refreshAuthorityUrl } = useRequest(
        () => getAuthorityUrl(authorityId),
        {
            ready: !!authorityId,
            manual: false,
            refreshDeps: [authorityId],
            onSuccess: (data) => {
                if (data) {
                    form.setFieldsValue({
                        urls: data,
                    });
                }
            }
        });

    const { runAsync: saveAuthorityUrlAsync, loading: saveAuthorityUrlLoading } = useRequest(saveAuthorityUrl, {
        manual: true
    })

    const { runAsync: deleteAuthorityUrlAsync, loading: deleteAuthorityUrlLoading } = useRequest(deleteAuthorityUrl, {
        manual: true
    })


    const handleSave = async (item, rowIndex) => {
        const formValues = await form.validateFields()
        const { urls } = formValues
        const { type } = item
        let req
        if(type === 'add'){
            req = {...item,authorityId: authorityId, id: null}
        } else {
            req = {...item}
        }
        await saveAuthorityUrlAsync(authorityId, req)
        refreshAuthorityUrl()

    }

    const handleDelete = async (item, rowIndex) => {
        await deleteAuthorityUrlAsync(authorityId, item.id)
        refreshAuthorityUrl()

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
                const safeValue = Array.isArray(value)
                    ? value
                    : (value ? [value] : []);
                return <Select style={{ width: '100%' }} options={requestMethodOptions} value={safeValue} onChange={onChange} mode="multiple" allowClear />
            },
            render: (_, { method }) => {

                const renderSingleTag = (methodStr) => {
                    switch (methodStr?.toUpperCase()) {
                        case RequestMethod.GET:
                            return <Tag key={methodStr} color="green">GET</Tag>
                        case RequestMethod.POST:
                            return <Tag key={methodStr} color="blue">POST</Tag>
                        case RequestMethod.PUT:
                            return <Tag key={methodStr} color="orange">PUT</Tag>
                        case RequestMethod.PATCH:
                            return <Tag key={methodStr} color="yellow">PATCH</Tag>
                        case RequestMethod.DELETE:
                            return <Tag key={methodStr} color="red">DELETE</Tag>
                        case RequestMethod.ALL:
                            return <Tag key={methodStr} color="purple">ALL</Tag>
                        default:
                            return <Tag key={methodStr} color="gray">{methodStr || '未知'}</Tag>
                    }
                };

                if (!method || method.length === 0) {
                    return <Tag color="gray">无</Tag>;
                }

                if (Array.isArray(method)) {
                    return (
                        <Flex justify='center' align='center'>
                            {method.map(m => renderSingleTag(m))}
                        </Flex>
                    );
                }

                return renderSingleTag(method)
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
                            loading={getAuthorityUrlLoading}
                        />
                    )}
                </Form.List>
            </Flex>
        </Form>
    )

}

export default AuthorityUrl