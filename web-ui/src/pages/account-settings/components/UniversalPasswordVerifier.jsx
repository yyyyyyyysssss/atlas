import React from 'react';
import { Form, Input, Typography, theme } from 'antd';
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
    label = "账户登录密码", 
    placeholder = "请输入当前账户的登录密码",
    errorMsg = "密码错误，请重新输入"
}) => {
    const { token } = theme.useToken();
    const [form] = Form.useForm();

    if (verifierRef) {
        verifierRef.current = {
            onVerify: async () => {
                // 1. 触发表单本身规则校验（若为空，会被 catch 拦截）
                try {
                    await form.validateFields(['password']);
                } catch (formError) {
                    throw new Error("请输入密码"); 
                }

                const values = form.getFieldValue('password');
                
                // 2. 执行外部传入的 API
                const verify = await onVerifyAction(values);
                
                // 3. 校验不通过，抛出异常阻断父组件
                if (!verify) {
                    throw new Error(errorMsg);
                }
                return true;
            }
        };
    }

    return (
        <Form form={form} layout="vertical" requiredMark={false}>
            <Form.Item
                name="password"
                label={<Text style={{ fontWeight: 500, fontSize: 13 }}>{label}</Text>}
                rules={[{ required: true, message: '密码不能为空' }]}
            >
                <Input.Password 
                    size="large" 
                    placeholder={placeholder} 
                    style={{ borderRadius: 8 }} 
                    prefix={<LockOutlined style={{ color: token.colorTextPlaceholder }} />}
                />
            </Form.Item>
        </Form>
    );
};

export default UniversalPasswordVerifier;