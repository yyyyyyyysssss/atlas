import React, { useState } from 'react';
import { Button, Typography, Flex, theme } from 'antd';
import { LockOutlined } from '@ant-design/icons';

const { Text } = Typography;

const PasswordItem = ({ passwordSet, refresh }) => {
    const { token } = theme.useToken();
    
    // 💡 内部控制修改/设置密码的弹窗或状态
    const [isModalOpen, setIsModalOpen] = useState(false);

    const handleActionClick = () => {
        // 这里可以打开你现有的修改密码 Modal 或跳转路由
        setIsModalOpen(true);
        // demo 提示，你可以换成你真正的弹窗逻辑
        console.log('打开密码修改/设置弹窗');
    };

    return (
        <>
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
                        <LockOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>登录密码</Text>
                        <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                            {passwordSet
                                ? '已设置。建议您定期更改密码以提升账号安全性。'
                                : '未设置。请尽快设置密码以确保基础登录安全。'}
                        </Text>
                    </Flex>
                </Flex>

                {/* 右侧：操作按钮 */}
                <Flex align="center" style={{ justifyContent: 'flex-end' }}>
                    <Button 
                        type={passwordSet ? "default" : "primary"} 
                        onClick={handleActionClick}
                    >
                        {passwordSet ? '修改密码' : '设置密码'}
                    </Button>
                </Flex>
            </Flex>

            {/* 💡 如果有修改密码的单独 Modal 组件，直接挂载在下面，完全不污染外层 */}
            {/* <PasswordChangeModal open={isModalOpen} onClose={() => setIsModalOpen(false)} onSuccess={refresh} /> */}
        </>
    );
};

export default PasswordItem;