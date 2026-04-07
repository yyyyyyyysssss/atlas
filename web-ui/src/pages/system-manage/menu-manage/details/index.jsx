import { Space, Flex, Form, Input, Button, Popconfirm, Row, Col, InputNumber, App } from 'antd'
import { UploadOutlined } from '@ant-design/icons';
import { OperationMode } from '../../../../enums/common';
import { useEffect, useState } from 'react';
import { createMenu, fetchMenuDetails, updateMenu } from '../../../../services/SystemService';
import HasPermission from '../../../../components/HasPermission';
import { useRequest } from 'ahooks';
import Loading from '../../../../components/loading';
import { useTranslation } from 'react-i18next';
import SmartUpload from '../../../../components/smart-upload';
import MenuAuthority from './MenuAuthority';

const MenuDetails = ({ menuId, parentId, parentCode, operationMode, changeOperationMode, onSuccess }) => {

    const { t } = useTranslation()

    const [form] = Form.useForm()

    const [menuData, setMenuData] = useState()

    const { message } = App.useApp()

    const { runAsync: fetchMenuDetailsAsync, loading: fetchMenuDetailsLoading } = useRequest(fetchMenuDetails, {
        manual: true
    })

    const { runAsync: createMenuAsync, loading: createMenuLoading } = useRequest(createMenu, {
        manual: true
    })

    const { runAsync: updateMenuAsync, loading: updateMenuLoading } = useRequest(updateMenu, {
        manual: true
    })

    useEffect(() => {

        const fetchData = async (menuId) => {
            if (!menuId) {
                return
            }
            const menuData = await fetchMenuDetailsAsync(menuId)
            form.setFieldsValue({ ...menuData, parentCode: parentCode })
            setMenuData(menuData)
        }
        switch (operationMode) {
            case OperationMode.VIEW.value:
                fetchData(menuId)
                break
            case OperationMode.ADD.value:
                form.resetFields()
                form.setFieldsValue({
                    parentCode: parentCode,
                    parentId: parentId
                })
                break
            case OperationMode.EDIT.value:
                break
        }
    }, [operationMode, menuId, form, parentCode])

    if (!operationMode) {
        return null
    }

    const resetForm = () => {
        form.resetFields()
    }

    const saveMenu = async () => {
        const formValues = await form.validateFields()
        const menuInfo = {
            ...formValues,
            code: operationMode === OperationMode.ADD.value && parentCode ? `${parentCode}:${formValues.code}` : formValues.code,
        }
        let menuId
        if (operationMode === OperationMode.ADD.value) {
            menuId = await createMenuAsync(menuInfo)
        } else {
            await updateMenuAsync(menuInfo)
            menuId = menuInfo.id
        }
        message.success('保存成功')
        onSuccess(menuId)
    }

    return (
        <Loading spinning={fetchMenuDetailsLoading || createMenuLoading || updateMenuLoading}>
            <Flex
                className='w-full'
                gap={20}
                vertical
            >
                <Flex
                    hidden={!(operationMode === OperationMode.VIEW.value)}
                >
                    <HasPermission hasPermissions='system:menu:write'>
                        <Button type="primary" onClick={() => changeOperationMode(OperationMode.EDIT.value)}>{t('编辑')}</Button>
                    </HasPermission>
                </Flex>
                <Form
                    form={form}
                    style={{ width: '100%' }}
                    labelCol={{ span: 2 }}
                    wrapperCol={{ span: 20 }}
                    layout="horizontal"
                    disabled={operationMode === OperationMode.VIEW.value}
                >
                    <Form.Item name="id" noStyle />
                    <Form.Item name="parentId" noStyle />
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="菜单名称"
                                name="name"
                                rules={[
                                    {
                                        required: true,
                                        message: `菜单名称不能为空`,
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
                                label="菜单编码"
                                name="code"
                                rules={[
                                    {
                                        required: true,
                                        message: `菜单编码不能为空`,
                                    },
                                ]}
                            >
                                <Input prefix={operationMode === OperationMode.ADD.value && parentCode ? `${parentCode}:` : ''} />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="路由"
                                name="routePath"
                                rules={[
                                    {
                                        required: true,
                                        message: `路由不能为空`,
                                    },
                                ]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item label="图标" name="icon">
                                <SmartUpload
                                    accept=".svg,.png,.jpg,.jpeg"
                                    maxCount={1}
                                >
                                    <Button icon={<UploadOutlined />}>{t('点击上传')}</Button>
                                </SmartUpload>
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
                </Form>
                <MenuAuthority
                    style={{ display: operationMode === OperationMode.VIEW.value ? 'block' : 'none' }}
                    menuId={menuId}
                    parentCode={menuData?.code}
                />
                <Flex
                    justify='flex-end'
                    align='center'
                    style={{
                        position: 'sticky',
                        bottom: 0,
                        left: 0,
                        width: '100%',
                        padding: '10px'
                    }}
                >
                    <Space>
                        {operationMode !== OperationMode.VIEW.value && (
                            <Button loading={createMenuLoading || updateMenuLoading} onClick={saveMenu} type="primary">{t('提交')}</Button>
                        )}
                        {operationMode === OperationMode.EDIT.value && (
                            <Button onClick={() => changeOperationMode(OperationMode.VIEW.value)}>{t('取消')}</Button>
                        )}
                        {operationMode === OperationMode.ADD.value && (
                            <Popconfirm
                                okText={t('确定')}
                                cancelText={t('取消')}
                                title={t('确定重置')}
                                onConfirm={resetForm}
                                style={{ marginInlineEnd: 8 }}
                            >
                                <Button>{t('重置')}</Button>
                            </Popconfirm>
                        )}
                    </Space>
                </Flex>
            </Flex>
        </Loading>
    )
}

export default MenuDetails