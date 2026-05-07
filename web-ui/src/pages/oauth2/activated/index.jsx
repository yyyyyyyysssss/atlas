import { Flex, Typography, Result, Button, theme, Card } from "antd"
import { CheckCircleFilled } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import useFullParams from '../../../hooks/useFullParams'

const Activated = () => {
    const { token } = theme.useToken();
    const navigate = useNavigate();
    const params = useFullParams();

    return (
        <Flex style={{ minHeight: '100vh', backgroundColor: token.colorBgContainer }} justify="center" align="center">
            <Card
                style={{
                    width: '100%',
                    maxWidth: '500px',
                    borderRadius: '20px',
                    boxShadow: token.boxShadowTertiary,
                    margin: '16px'
                }}
            >
                <Result
                    icon={<CheckCircleFilled style={{ color: token.colorSuccess }} />}
                    title={
                        <Typography.Title level={3} style={{ margin: 0 }}>
                            设备已成功连接
                        </Typography.Title>
                    }
                    subTitle={
                        <Flex vertical gap={8} style={{ marginTop: 16 }}>
                            <Typography.Text type="secondary" style={{ fontSize: 16 }}>
                                您已成功授权该设备。
                            </Typography.Text>
                            <Typography.Text type="secondary">
                                现在您可以安全地关闭此窗口，并返回您的设备继续操作。
                            </Typography.Text>
                        </Flex>
                    }
                    extra={[
                        <Button
                            key="home"
                            type="primary"
                            onClick={() => navigate('/')}
                        >
                            返回首页
                        </Button>
                    ]}
                />
            </Card>
        </Flex>
    )
}

export default Activated