import { Button, Col, Drawer, Flex, Form, Input, InputNumber, Modal, Popconfirm, Row, Space, Table, Tag, Typography } from "antd"
import HasPermission from "../../../../components/HasPermission"
import { useTranslation } from "react-i18next"
import { AuthorityType, OperationMode, RequestMethod } from "../../../../enums/common"
import { useRequest } from "ahooks"
import { addAuthority, deleteAuthorityById, fetchAuthorityByMenuId, updateAuthority, updateAuthorityUrlsById } from "../../../../services/SystemService"
import { useEffect, useState } from "react"
import EditableTable from "../../../../components/smart-table/EditableTable"
import { getMessageApi } from "../../../../utils/MessageUtil"
import AuthorityUrl from "./AuthorityUrl"


const MenuAuthority = ({ style, menuId, parentCode }) => {

    const { t } = useTranslation()

    const [form] = Form.useForm()

    const { runAsync: getAuthorityByMenuIdAsync, loading: getAuthorityByMenuIdLoading } = useRequest(fetchAuthorityByMenuId, {
        manual: true
    })

    const { runAsync: addAuthorityAsync, loading: addAuthorityLoading } = useRequest(addAuthority, {
        manual: true
    })

    const { runAsync: updateAuthorityAsync, loading: updateAuthorityLoading } = useRequest(updateAuthority, {
        manual: true
    })

    const { runAsync: deleteAuthorityByIdAsync, loading: deleteAuthorityByIdLoading } = useRequest(deleteAuthorityById, {
        manual: true
    })

    const { runAsync: updateAuthorityUrlsByIdAsync, loading: updateAuthorityUrlsByIdLoading } = useRequest(updateAuthorityUrlsById, {
        manual: true
    })

    const [authorityList, setAuthorityList] = useState([])

    const [authorityModalOpen, setAuthorityModalOpen] = useState({
        open: false,
        operationMode: null,
        title: ''
    })

    const [authorityApiDrawerOpen, setAuthorityApiDrawerOpen] = useState({
        open: false,
        authorityId: null,
        authorityUrls: null,
        title: ''
    })


    const fetchData = async (menuId) => {
        if (!menuId) {
            return
        }
        const data = await getAuthorityByMenuIdAsync(menuId)
        setAuthorityList(data)
    }

    useEffect(() => {
        fetchData(menuId)
    }, [menuId])

    if (!menuId) {
        return null
    }

    const handleSaveMenuAuthority = async () => {
        const formValues = await form.validateFields()
        const requestParam = {
            ...formValues,
            code: authorityModalOpen.operationMode === OperationMode.ADD.value && parentCode ? `${parentCode}:${formValues.code}` : formValues.code,
            parentId: menuId
        }
        if (authorityModalOpen.operationMode === OperationMode.ADD.value) {
            await addAuthorityAsync(requestParam)
            getMessageApi().success(t('新增成功'))
        } else {
            await updateAuthorityAsync(requestParam)
            getMessageApi().success(t('修改成功'))
        }
        handleClose()
        fetchData(menuId)
    }

    const handleAddAuthority = () => {
        form.setFieldValue('parentId', menuId)
        setAuthorityModalOpen({
            open: true,
            operationMode: OperationMode.ADD.value,
            title: '新增权限'
        })
    }

    const handleEditAuthority = (authorityData) => {
        form.setFieldsValue({ ...authorityData })
        setAuthorityModalOpen({
            open: true,
            operationMode: OperationMode.EDIT.value,
            title: '修改权限'
        })
    }

    const handleDeleteAuthority = async (authorityId) => {
        await deleteAuthorityByIdAsync(authorityId)
        getMessageApi().success(t('删除成功'))
        handleClose()
        fetchData(menuId)
    }

    const handleClose = () => {
        setAuthorityModalOpen({
            open: false,
            operationMode: null,
            title: ''
        })
    }

    const showDrawer = (authorityId) => {
        const authorityData = authorityList.find(item => item.id === authorityId)
        const newAuthorityUrls = authorityData.urls.map((item, index) => {
            return {
                ...item,
                id: index
            }
        })
        setAuthorityApiDrawerOpen({
            open: true,
            title: authorityData.name,
            authorityId: authorityId,
            authorityUrls: newAuthorityUrls
        })
    }

    const handleAuthorityUrlChange = async (newAuthorityUrls) => {
        const authorityId = authorityApiDrawerOpen.authorityId
        await updateAuthorityUrlsByIdAsync(authorityId, newAuthorityUrls)
        setAuthorityList((prevList) => {
            return prevList.map((item) => {
                if (item.id === authorityId) {
                    return { ...item, urls: newAuthorityUrls } // 更新对应项的 url
                }
                return item // 其他项保持不变
            })
        })
        getMessageApi().success(t('修改成功'))
    }

    const handleDrawerClose = () => {
        setAuthorityApiDrawerOpen({
            open: false,
            authorityId: null,
            authorityUrls: null,
            title: null
        })
    }

    const columns = [
        {
            key: 'name',
            title: '权限名称',
            dataIndex: 'name',
            align: 'center',
        },
        {
            key: 'code',
            title: '权限编码',
            dataIndex: 'code',
            align: 'center',
        },
        {
            key: 'type',
            title: '权限类型',
            dataIndex: 'type',
            align: 'center',
            render: (_, { type }) => {
                let color
                let text
                if (type === AuthorityType.BUTTON) {
                    color = '#1890ff'
                    text = '按钮'
                } else if (type === AuthorityType.API) {
                    color = '#722ed1'
                    text = 'API'
                } else {
                    color = 'gray'
                    text = '未知'
                }
                return (
                    <Tag color={color}>
                        {text}
                    </Tag>
                )
            }

        },
        {
            key: 'action',
            title: '操作',
            dataIndex: 'action',
            align: 'center',
            render: (_, record) => {
                return (
                    <Space size='middle'>
                        <HasPermission hasPermissions='system:menu:read'>
                            <Typography.Link onClick={() => showDrawer(record.id)}>{t('API权限')}</Typography.Link>
                        </HasPermission>
                        <HasPermission hasPermissions='system:menu:write'>
                            <Typography.Link onClick={() => handleEditAuthority(record)}>{t('编辑')}</Typography.Link>
                        </HasPermission>
                        <HasPermission hasPermissions='system:menu:delete'>
                            <Popconfirm okText={t('确定')} cancelText={t('取消')} title={t('确定删除')} okButtonProps={{ loading: deleteAuthorityByIdLoading }} onConfirm={async () => await handleDeleteAuthority(record.id)} style={{ marginInlineEnd: 8 }}>
                                <Typography.Link>
                                    {t('删除')}
                                </Typography.Link>
                            </Popconfirm>
                        </HasPermission>
                    </Space>
                )
            }
        }
    ]

    const urlColumns = [
        {
            key: 'method',
            title: '请求方法',
            dataIndex: 'method',
            align: 'center',
            width: '32%',
            editable: true,
            inputType: 'select',
            options: Object.entries(RequestMethod).map(([key, value]) => ({
                label: key,
                value: value,
            })),
            required: true,
        },
        {
            key: 'url',
            title: '请求路径',
            dataIndex: 'url',
            align: 'center',
            width: '45%',
            editable: true,
            required: true,
        }
    ]

    return (
        <Flex
            style={style}
        >
            <Flex
                gap={20}
                vertical
            >
                <HasPermission hasPermissions='system:menu:write'>
                    <Button type="primary" onClick={() => handleAddAuthority()} className='w-28'>{t('新增权限')}</Button>
                </HasPermission>
                <Table
                    columns={columns}
                    loading={getAuthorityByMenuIdLoading}
                    dataSource={authorityList}
                    rowKey={(record) => record.id}
                    pagination={false}
                />
            </Flex>
            <Modal
                title={authorityModalOpen.title}
                width={500}
                centered
                open={authorityModalOpen.open}
                confirmLoading={addAuthorityLoading || updateAuthorityLoading}
                onOk={handleSaveMenuAuthority}
                onCancel={handleClose}
                onClose={handleClose}
                maskClosable={false}
                keyboard={false}
                okText={t('保存')}
                cancelText={t('取消')}
                afterClose={() => form.resetFields()}
            >
                <Flex
                    style={{ marginTop: '20px', height: 500 }}
                    gap={10}
                    vertical
                >
                    <Form
                        form={form}
                        labelCol={{ span: 6 }}
                        wrapperCol={{ span: 18 }}
                        layout="horizontal"
                    >
                        <Form.Item name="id" noStyle />
                        <Form.Item name="parentId" noStyle />
                        <Form.Item name="rootId" noStyle />
                        <Row gutter={16}>
                            <Col span={24}>
                                <Form.Item
                                    label="权限名称"
                                    name="name"
                                    rules={[
                                        {
                                            required: true,
                                            message: `权限名称不能为空`,
                                        },
                                    ]}
                                >
                                    <Input />
                                </Form.Item>
                            </Col>
                        </Row>
                        <Row gutter={16}>
                            <Col span={24}>
                                <Form.Item
                                    label="权限编码"
                                    name="code"
                                    rules={[
                                        {
                                            required: true,
                                            message: `权限名称不能为空`,
                                        },
                                    ]}
                                >
                                    <Input prefix={authorityModalOpen.operationMode === OperationMode.ADD.value && parentCode ? `${parentCode}:` : ''} />
                                </Form.Item>
                            </Col>
                        </Row>
                        <Row gutter={16}>
                            <Col span={24}>
                                <Form.Item label="排序" name="sort">
                                    <InputNumber
                                        min={1}
                                        step={1}
                                        style={{ width: '100%' }}
                                    />
                                </Form.Item>
                            </Col>
                        </Row>
                        <Row gutter={16}>
                            <Col span={24}>
                                <Form.List
                                    name="urls"
                                    noStyle
                                >
                                    {(fields, { add, remove }) => (
                                        <EditableTable
                                            columns={urlColumns}
                                            name='urls'
                                            mode='multi-add'
                                            fields={fields}
                                            add={add}
                                            remove={remove}
                                            scroll={{
                                                y: 200
                                            }}
                                        />
                                    )}
                                </Form.List>
                            </Col>
                        </Row>
                    </Form>
                </Flex>
            </Modal>
            <Drawer
                title={t('API权限') + `[${authorityApiDrawerOpen.title}]`}
                closable={{ 'aria-label': 'Close Button' }}
                onClose={handleDrawerClose}
                open={authorityApiDrawerOpen.open}
                width={700}
            >
                <AuthorityUrl
                    authorityUrls={authorityApiDrawerOpen.authorityUrls}
                    onChange={handleAuthorityUrlChange}
                    loading={updateAuthorityUrlsByIdLoading}
                />
            </Drawer>
        </Flex>
    )
}

export default MenuAuthority