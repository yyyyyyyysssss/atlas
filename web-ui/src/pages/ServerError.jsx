import { Button, Result, Typography, theme, Flex, Space, Divider } from "antd"
import { useNavigate, useLocation } from 'react-router-dom'
import useFullParams from "../hooks/useFullParams"
import { 
    FileTextOutlined, 
    HomeOutlined,
    BugOutlined,
    QuestionCircleOutlined,
    BookOutlined
} from '@ant-design/icons';


const ServerError = () => {

    const navigate = useNavigate()
    const params = useFullParams();
    const { token } = theme.useToken();

    const title = params.error || params.title || '500';

    const subTitle = params.message || params.subTitle || '抱歉，页面出了点问题，请稍后再试';

    const docUrl = params.errorUri || params.supportDoc

    const goHome = () => {
        navigate('/')
    }

        return (
            <Flex 
                justify="center" 
                align="center" 
                style={{ 
                    minHeight: '100vh', 
                    backgroundColor: token.colorBgContainer 
                }}
            >
                <Result
                    status="500"
                    title={
                        <Typography.Title level={2} style={{ margin: 0, color: token.colorTextHeading }}>
                            {title}
                        </Typography.Title>
                    }
                    subTitle={
                        <Typography.Text type="secondary" style={{ fontSize: 16 }}>
                            {subTitle}
                        </Typography.Text>
                    }
                    extra={
                        <Button onClick={goHome} type="primary" key="home" size="large">
                            返回主页
                        </Button>
                    }
                >
                    {docUrl && (
                        <div>
                            <Typography.Title level={5} style={{ marginTop: 0, marginBottom: 16, color: token.colorTextHeading }}>
                                排障建议
                            </Typography.Title>
                        
                            <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
                                <BugOutlined style={{ marginRight: 8 }} />
                                如果您是开发者，可以通过系统日志排查报错原因。
                            </Typography.Paragraph>
                        
                            <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
                                <BookOutlined style={{ marginRight: 8 }} />
                                您可以查阅相关的<Typography.Link href={docUrl} target="_blank" style={{ marginLeft: 4 }}>官方技术文档</Typography.Link>以获取解决方案。
                            </Typography.Paragraph>
                        </div>
                    )}
                </Result>
            </Flex>
        )
}

export default ServerError