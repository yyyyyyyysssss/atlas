import React from 'react';
import './index.css'
import { Flex, Form, Input, Radio } from 'antd';
import RoleSelect from '../../../../components/RoleSelect';
import OptionTreeSelect from '../../../../components/OptionTreeSelect';
// import useStateParams from '../../../../hooks/useStateParams';
// import { useParams } from 'react-router-dom';

const UserDetails = ({ userId, operationMode }) => {

    const [form] = Form.useForm()

    return (
        <Flex
            justify='center'
        >
            <Form
                form={form}
                layout="vertical"
                style={{ width: '30%' }}
            >
                <Row gutter={16}>
                    <Col span={24}>
                        <Form.Item
                            label="姓名"
                            name="fullName"
                        >
                            <Input placeholder="请输入用户姓名" />
                        </Form.Item>
                    </Col>
                </Row>
                <Row gutter={16}>
                    <Col span={24}>
                        <Form.Item
                            label="账号"
                            name="username"
                        >
                            <Input placeholder="请输入账号" />
                        </Form.Item>
                    </Col>
                </Row>
                <Row gutter={16}>
                    <Col span={24}>
                        <Form.Item
                            label="邮箱"
                            name="email"
                        >
                            <Input placeholder="请输入用户邮箱" />
                        </Form.Item>
                    </Col>
                </Row>
                <Row gutter={16}>
                    <Col span={24}>
                        <Form.Item
                            label="手机号"
                            name="phone"
                        >
                            <Input placeholder="请输入用户手机号" />
                        </Form.Item>
                    </Col>
                </Row>
                <Row gutter={16}>
                    <Col span={24}>
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
                    </Col>
                </Row>
                <Row gutter={16}>
                    <Col span={24}>
                        <Form.Item
                            label="所属组织"
                            name="orgId"
                            rules={[
                                {
                                    required: true,
                                    message: `所属组织不能为空`,
                                },
                            ]}
                        >
                            <OptionTreeSelect
                                fetchData={fetchOrgOptions}
                                multiple={false}
                            />
                        </Form.Item>
                    </Col>
                </Row>
                <Row gutter={16}>
                    <Col span={24}>
                        <Form.Item
                            label="分配角色"
                            name="roleIds"
                        >
                            <RoleSelect />
                        </Form.Item>
                    </Col>
                </Row>
            </Form>
        </Flex>
    )
}

export default UserDetails