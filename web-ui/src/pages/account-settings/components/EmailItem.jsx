import React from 'react';
import { Button, Typography, Flex, Tag, App, theme } from 'antd';
import { MailOutlined } from '@ant-design/icons';

const { Text } = Typography;

const EmailItem = ({ boundEmail, emailVerified, refresh }) => {
    const { token } = theme.useToken();
    const { modal } = App.useApp();

    const handleEmailAction = () => {
        modal.confirm({
            title: boundEmail ? '更换绑定邮箱' : '绑定电子邮箱',
            content: '为了您的账号安全，更换邮箱需要进行身份验证。',
            okText: '开始验证',
            cancelText: '取消',
            onOk: async () => {
                try {
                    // 💡 这里后续可以对接你的验证/换绑异步接口
                    console.log('触发验证流程');
                    // 成功后调用 refresh() 刷新父组件数据
                    // refresh();
                } catch (error) {
                    console.error('操作失败:', error);
                }
            }
        });
    };

    return (
        <Flex 
            justify="space-between" 
            align="center" 
            style={{ 
                padding: '20px 0', 
                borderBottom: `1px solid ${token.colorBorderSecondary}` 
            }}
        >
            {/* 左侧：图标 + 标题与描述 */}
            <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                <div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%', display: 'flex' }}>
                    <MailOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />
                </div>
                <Flex vertical gap={4}>
                    <Text strong style={{ fontSize: 16 }}>电子邮箱</Text>
                    <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                        {boundEmail
                            ? `已绑定：${boundEmail}。用于接收安全通知、重置密码及验证码登录。`
                            : '未绑定电子邮箱。绑定后可用于接收通知与找回密码。'}
                    </Text>
                </Flex>
            </Flex>

            {/* 右侧：标签 + 操作按钮 */}
            <Flex align="center" gap={12} style={{ justifyContent: 'flex-end', height: '100%' }}>
                {boundEmail && (
                    <Tag color={emailVerified ? "success" : "warning"} bordered={false}>
                        {emailVerified ? "已验证" : "未验证"}
                    </Tag>
                )}
                <Button
                    type={boundEmail ? "text" : "primary"}
                    size={boundEmail ? "small" : "middle"}
                    onClick={handleEmailAction}
                >
                    {boundEmail ? "修改" : "绑定"}
                </Button>
            </Flex>
        </Flex>
    );
};

export default EmailItem;