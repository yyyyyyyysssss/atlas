import React, { useState } from 'react';
import { App, Form, Input, Typography, theme } from 'antd';
import { LockOutlined } from '@ant-design/icons';

const { Text } = Typography;

/**
 * 全站通用的密码身份验证组件
 * @param {Object} verifierRef - 用于穿透绑定的 ref
 * @param {Function} onVerifyAction - 真正的后端 API 验证请求（由父组件决定调用哪个接口），需返回 Promise<boolean>
 * @param {string} label - 输入框上方的标签文案（支持自定义）
 * @param {string} placeholder - 占位提示文案
 * @param {string} errorMsg - 密码错误时的提示文案
 */
const UniversalPasswordVerifier = ({
    verifierRef,
    onVerifyAction,
    onSuccess,
    label = "账户登录密码",
    placeholder = "请输入当前账户的登录密码",
    errorMsg = "密码错误，请重新输入"
}) => {
    const { token } = theme.useToken();
    const [form] = Form.useForm();

    const { message } = App.useApp()

    const [loading, setLoading] = useState(false)


    const handlePasswordVerify = async () => {
        setLoading(true)
        try {
            await form.validateFields(['password'])

            const values = form.getFieldValue('password');

            // 2. 执行外部传入的 API
            const result = await onVerifyAction(values);

            // 3. 校验不通过，抛出异常阻断父组件
            if (!result || !result.verified) {
                throw new Error(errorMsg);
            }
            if (onSuccess) {
                onSuccess(result.ticket);
            }
            return { verified: result.verified, ticket: result.ticket }
        } finally {
            setLoading(false)
        }

    }

    const handleInternalTrigger = async () => {
        try {
            await handlePasswordVerify()
        } catch (error) {
            message.error(error.message);
        }
    }

    if (verifierRef) {
        verifierRef.current = {
            getValue: () => {
                return form.getFieldValue('password');
            },
            validate: async () => {
                return await form.validateFields(['password']);
            },
            onVerify: async () => {

                return await handlePasswordVerify()
            },
            reset: () => {
                form.resetFields();
            }
        };
    }

    return (
        <Form form={form} layout="vertical" requiredMark={false} component={false}>
            <Form.Item
                name="password"
                label={<Text style={{ fontWeight: 500, fontSize: 13 }}>{label}</Text>}
                rules={[{ required: true, message: '密码不能为空' }]}
            >
                <Input.Password
                    size="large"
                    disabled={loading}
                    placeholder={placeholder}
                    style={{ borderRadius: 8 }}
                    prefix={<LockOutlined style={{ color: token.colorTextPlaceholder }} />}
                    onPressEnter={handleInternalTrigger}
                />
            </Form.Item>
        </Form>
    );
};

export default UniversalPasswordVerifier