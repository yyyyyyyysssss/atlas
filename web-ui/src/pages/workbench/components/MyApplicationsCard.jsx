import React from 'react';
import {
    Card,
    Typography,
    Flex,
    Row,
    Col,
    Tag,
    theme,
    Button,
} from 'antd';
import { ArrowUpRight, CheckCircle2, AppWindow, UserRound, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const { Text } = Typography;

import atlasLogo from '/logo128_eclipse.svg';

const MyApplicationsCard = () => {
    const { token } = theme.useToken();
    const navigate = useNavigate();

    const applications = [
        {
            key: 'atlas',
            name: 'Atlas',
            category: '身份中心',
            description: '统一身份认证与账号管理，保障数字身份安全。',
            logo: atlasLogo,
            status: '当前平台',
            link: 'https://atlas.ys0921.sbs:4443'
        },
    ];

    const gotoApp = (app) => {
        window.open(app.link, '_blank')
    }

    return (
        <Card
            title={
                <Flex align="center" gap={8}>
                    <AppWindow size={18} color={token.colorPrimary} />
                    <Text strong>我的应用</Text>
                </Flex>
            }
            variant="borderless"
        >
            <Row gutter={[16, 16]}>
                {applications.map((app) => (
                    <Col xs={24} sm={12} lg={8} key={app.key}>
                        <div
                            onClick={() => gotoApp(app)}
                            style={{
                                boxSizing: 'border-box',
                                width: '100%',
                                minWidth: 0,
                                height: 160,
                                padding: 16,
                                cursor: 'pointer',
                                borderRadius: token.borderRadiusLG,
                                border: `1px solid ${token.colorBorderSecondary}`,
                                background: token.colorBgContainer,
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'space-between',
                                gap: 12,
                                overflow: 'hidden',
                                transition: 'all 0.2s ease',
                            }}
                        >
                            <Flex vertical gap={12} style={{ minWidth: 0 }}>
                                <Flex
                                    align="center"
                                    justify="space-between"
                                    gap={8}
                                >
                                    <Flex
                                        align="center"
                                        gap={10}
                                        style={{ minWidth: 0 }}
                                    >
                                        <img
                                            src={app.logo}
                                            alt={`${app.name} Logo`}
                                            style={{
                                                width: 42,
                                                height: 42,
                                                objectFit: 'contain',
                                                flexShrink: 0,
                                            }}
                                        />

                                        <Text
                                            strong
                                            style={{ fontSize: 16, minWidth: 0 }}
                                            ellipsis
                                        >
                                            {app.name}
                                        </Text>
                                    </Flex>

                                    <Tag
                                        color="success"
                                        bordered={false}
                                        style={{
                                            marginInlineEnd: 0,
                                            flexShrink: 0,
                                        }}
                                    >
                                        {app.category}
                                    </Tag>
                                </Flex>

                                <Text
                                    type="secondary"
                                    style={{
                                        fontSize: 12,
                                        lineHeight: '20px',
                                        display: '-webkit-box',
                                        WebkitBoxOrient: 'vertical',
                                        WebkitLineClamp: 2,
                                        overflow: 'hidden',
                                        overflowWrap: 'anywhere',
                                    }}
                                >
                                    {app.description}
                                </Text>
                            </Flex>

                            <Flex
                                align="center"
                                justify="space-between"
                                gap={8}
                                style={{
                                    paddingTop: 10,
                                    borderTop: `1px solid ${token.colorBorderSecondary}`,
                                }}
                            >
                                <Flex align="center" gap={6}>
                                    <CheckCircle2
                                        size={14}
                                        color={token.colorSuccess}
                                    />
                                    <Text
                                        type="secondary"
                                        style={{ fontSize: 12 }}
                                    >
                                        {app.status}
                                    </Text>
                                </Flex>

                                <Flex align="center" justify="flex-end" gap={4} style={{ marginTop: 16 }}>
                                    <Text strong style={{ fontSize: 12, color: token.colorPrimary }}>
                                        访问
                                    </Text>
                                    <ArrowRight size={14} color={token.colorPrimary} />
                                </Flex>
                            </Flex>
                        </div>
                    </Col>
                ))}
            </Row>

            <Text
                type="secondary"
                style={{
                    display: 'block',
                    marginTop: 16,
                    fontSize: 12,
                    textAlign: 'center',
                }}
            >
                接入 Atlas 的业务应用将在此展示
            </Text>
        </Card>
    );
};

export default MyApplicationsCard;