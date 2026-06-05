import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode, theme } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled } from '@ant-design/icons';
import { passwordLogin } from '../../../../services/LoginService';
import { useRequest } from 'ahooks';
import { AnimatePresence, motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';


const PasswordLogin = ({ onSuccess }) => {

    const { t } = useTranslation()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const { runAsync: passwordLoginAsync, loading: passwordLoginLoading } = useRequest(passwordLogin, {
        manual: true
    })

    const onFinish = async (values) => {
        try {
            const loginResponse = await passwordLoginAsync({
                username: values.username,
                password: values.password,
                clientType: 'WEB',
            })
            onSuccess(loginResponse)
        } catch (error) {
            if (error.response && error.response.status === 401) {
                if (error.response.data && error.response.data.code === 2201) {
                    message.error('账号已锁定，请联系系统管理员')
                } else {
                    message.error('用户名或密码错误')
                }
            }
        }

    }


    return (
        <motion.div
            key="password-login"
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 10 }}
            transition={{ duration: 0.2 }}
        >
            <Form form={form} onFinish={onFinish} layout="vertical">
                <Form.Item name="username" rules={[{ required: true, message: '用户名不可为空' }]} style={{ marginBottom: 20 }}>
                    <Input
                        allowClear
                        size="large" placeholder="用户名或邮箱"
                        prefix={
                            <UserOutlined style={{ color: '#9ca3af', marginRight: 8 }} />
                        }
                    />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true, message: '密码不可为空' }]} style={{ marginBottom: 20 }}>
                    <Input.Password size="large" placeholder="密码" prefix={<LockOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                </Form.Item>
                <Form.Item style={{ marginBottom: 32, marginTop: 12 }}>
                    <Button type="primary" htmlType="submit" size="large" block loading={passwordLoginLoading} style={{ boxShadow: '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}>
                        {t('登 录')}
                    </Button>
                </Form.Item>
            </Form>
        </motion.div>
    )
}

export default PasswordLogin;