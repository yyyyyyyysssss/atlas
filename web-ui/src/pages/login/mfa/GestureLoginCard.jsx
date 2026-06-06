import { Card, Flex, Typography } from "antd";
import { Monitor } from "lucide-react";
import { LockFilled } from '@ant-design/icons'; // 引入一个安全的锁图标作为点缀
import UniversalGestureVerifier from "../../account-settings/components/verifiers/UniversalGestureVerifier";
import { useRef } from "react";
import { gestureLogin } from "../../../services/LoginService";
import { useRequest } from "ahooks";
import { useTranslation } from 'react-i18next';

const GestureLoginCard = ({ loginPanel, setLoginPanel, loginSuccessHandler }) => {
    const { t } = useTranslation();
    const verifierRef = useRef(null);

    const { runAsync: gestureLoginAsync, loading: gestureLoginLoading } = useRequest(gestureLogin, {
        manual: true
    })

    const login = () => {
        // 预留登录逻辑
    }

    return (
        <Card
            style={{
                width: '100%',
                height: '100%',
                borderRadius: '24px',
                boxShadow: '0 25px 50px -12px rgba(0,0,0,0.05)',
                border: 'none',
                background: '#ffffff',
                position: 'relative',
            }}
            styles={{
                body: {
                    height: '100%',
                }
            }}
        >
            {/* 右上角切回电脑图标 */}
            <div
                style={{
                    position: 'absolute',
                    top: 16,
                    right: 16,
                    cursor: 'pointer',
                    zIndex: 10,
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                }}
                onClick={() => setLoginPanel('main')}
                title="密码登录"
            >
                <Monitor size={32} color="#9ca3af" strokeWidth={1.5} />
            </div>

            {/* 主内容 Flex 容器：通过 gap 紧凑排布，彻底杜绝溢出 */}
            <Flex
                vertical
                align="center"
                justify="center"
                style={{ height: '100%' }}
                gap={12} // 统一控制上下间距，避免使用大的 marginBottom
            >
                {/* 1. 轻量化头部图标与标题 */}
                <Flex vertical align="center" style={{ marginBottom: 4, marginTop: 36 }}>
                    <Typography.Title level={4} style={{ margin: 0, fontWeight: 600, color: '#111827' }}>
                        {t('手势轨迹登录')}
                    </Typography.Title>
                    <Typography.Text type="secondary" style={{ fontSize: 13, marginTop: 2 }}>
                        {t('请绘制解锁图案以验证身份')}
                    </Typography.Text>
                </Flex>

                {/* 2. 手势区域：不再包裹任何带有 padding 的复杂容器，直接裸展组件 */}
                <div
                    style={{
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        width: '100%',
                        padding: '20px',
                    }}
                >
                    <UniversalGestureVerifier
                        label=""
                        verifierRef={verifierRef}
                        onVerifyAction={login}
                        onSuccess={loginSuccessHandler}
                    />
                </div>

                {/* 3. 极简页脚提示 */}
                <Typography.Text type="secondary" style={{ fontSize: 12, textAlign: 'center', width: '100%', marginBottom: 20 }}>
                    忘记手势？
                    <Typography.Link onClick={() => setLoginPanel('main')} style={{ color: '#4f46e5' }}>
                        切换密码登录
                    </Typography.Link>
                </Typography.Text>
            </Flex>
        </Card>
    );
};

export default GestureLoginCard;