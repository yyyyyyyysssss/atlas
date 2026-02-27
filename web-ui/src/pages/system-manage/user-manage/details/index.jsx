import React from 'react';
import './index.css'
import { Button, Card, Flex, Form, Input, Radio, Select } from 'antd';
import useBack from '../../../../hooks/useBack';
import useFullParams from '../../../../hooks/useFullParams';
// import useStateParams from '../../../../hooks/useStateParams';
// import { useParams } from 'react-router-dom';

const UserDetails = () => {

    const [form] = Form.useForm()

    const { id, type } = useFullParams()


    const { goBack } = useBack()

    const handleClose = () => {
        goBack()
    }

    return (
        <Flex
            justify='center'
        >
            <Form
                form={form}
                layout="vertical"
                style={{ width: '30%' }}
            >
                <Form.Item
                    label="姓名"
                    name="fullName"
                >
                    <Input placeholder="请输入用户姓名" />
                </Form.Item>
                <Form.Item
                    label="账号"
                    name="username"
                >
                    <Input placeholder="请输入用户账号" />
                </Form.Item>
                <Form.Item
                    label="邮箱"
                    name="email"
                >
                    <Input placeholder="请输入用户邮箱" />
                </Form.Item>
                <Form.Item
                    label="手机号"
                    name="phone"
                >
                    <Input placeholder="请输入用户手机号" />
                </Form.Item>
                <Form.Item
                    label="启用状态"
                    name="enabled"
                    rules={[
                        {
                            required: true,
                            message: `启用状态不能为空`,
                        },
                    ]}
                >
                    <Radio.Group
                        options={[
                            { value: true, label: '启用' },
                            { value: false, label: '停用' }
                        ]}
                    />
                </Form.Item>
                <Form.Item
                    label="绑定角色"
                    name="roleIds"
                >
                    <Select placeholder="请选择角色" />
                </Form.Item>
            </Form>
        </Flex>
    )
}

export default UserDetails