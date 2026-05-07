


import { Flex, Typography, Button, theme, Card, Input, Form, message } from "antd"
import { MonitorOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import Cookies from 'js-cookie'
import httpWrapper from '../../../services/AxiosWrapper'
import useFullParams from "../../../hooks/useFullParams"

const Activate = () => {

    const { token } = theme.useToken()

    const { user_code } = useFullParams()

    const [form] = Form.useForm()

    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (user_code) {
            form.setFieldsValue({ userCode: user_code })
        }
    }, [user_code, form])

    const onFinish = (values) => {
        const code = values.userCode?.trim()

        if (!code) {
            message.warning("请输入设备上显示的验证码");
            return;
        }
        setLoading(true)
        // 设备码通常不区分大小写，统一转为大写处理
        const formattedCode = code.toUpperCase();
        const requestUrl = `${httpWrapper.getUri()}/api/auth/oauth2/device_verification?user_code=${code}&access_token=${Cookies.get("accessToken")}`;
        window.location.href = requestUrl;
    };

    return (
        <Flex style={{ minHeight: '100vh', backgroundColor: token.colorBgContainer }} justify="center" align="center">
            <Card
                style={{
                    width: '100%',
                    maxWidth: '480px',
                    borderRadius: '20px',
                    boxShadow: token.boxShadowTertiary,
                    margin: '16px'
                }}
            >
                <Flex vertical align="center" gap="large">
                    <Flex justify="center" align="center" style={{
                        width: 64,
                        height: 64,
                        borderRadius: '50%',
                        backgroundColor: token.colorFillAlter,
                        marginBottom: 8
                    }}>
                        <MonitorOutlined style={{ fontSize: 32, color: token.colorPrimary }} />
                    </Flex>

                    <Flex vertical align="center" gap={8} style={{ textAlign: 'center', marginBottom: 16 }}>
                        <Typography.Title level={3} style={{ margin: 0 }}>
                            连接设备
                        </Typography.Title>
                        <Typography.Text type="secondary" style={{ fontSize: 16 }}>
                            请输入您在智能电视、游戏主机或其他设备屏幕上看到的连接代码。
                        </Typography.Text>
                    </Flex>

                    <Form
                        form={form}
                        name="activate_device"
                        onFinish={onFinish}
                        style={{ width: '100%' }}
                        layout="vertical"
                    >
                        <Form.Item
                            name="userCode"
                            rules={[{ required: true, message: '请输入设备连接码' }]}
                        >
                            <Input
                                size="large"
                                placeholder="例如: XCGK-HKWV"
                                style={{
                                    textAlign: 'center',
                                    fontSize: 20,
                                    letterSpacing: '2px',
                                    borderRadius: token.borderRadiusLG
                                }}
                                maxLength={10}
                            />
                        </Form.Item>

                        <Form.Item style={{ marginTop: 32, marginBottom: 0 }}>
                            <Button
                                type="primary"
                                htmlType="submit"
                                loading={loading}
                                size="large"
                                block
                                style={{ borderRadius: token.borderRadiusLG }}
                            >
                                下一步
                            </Button>
                        </Form.Item>
                    </Form>
                </Flex>
            </Card>
        </Flex>
    )
}

export default Activate