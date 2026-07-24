import React from 'react';
import { Card, Typography, Flex, Button, theme, Row, Col, Tag } from 'antd';
import { 
    ShieldCheck, 
    ExternalLink, 
    ArrowRight, 
    KeyRound, 
    UserCheck, 
    Building2, // 新增：代表企业级 SAML 2.0 的图标
    CheckCircle2
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const { Text } = Typography;

const IntegrationGuideCard = () => {
    const { token } = theme.useToken();
    const navigate = useNavigate();

    const docsList = [
        {
            key: 'oauth21',
            title: 'OAuth 2.1 授权规范',
            tag: 'OAuth 2.1',
            tagColor: 'blue',
            icon: <KeyRound size={20} color={token.colorPrimary} />,
            description: '基于最新 OAuth 2.1 标准，整合最佳安全实践，全面保障客户端与 API 安全。',
            features: [
                '强制启用 PKCE 防御截获攻击',
                '废弃 Implicit & Password 模式',
                '重定向地址严格精确匹配'
            ],
            link: '/docs/oauth21-guide',
        },
        {
            key: 'oidc',
            title: 'OIDC 身份认证',
            tag: 'OIDC 1.0',
            tagColor: 'purple',
            icon: <UserCheck size={20} color={token.colorPurple || token.colorPrimary} />,
            description: '基于 OAuth 2.1 之上的身份层，通过 ID Token 快速解析与验证用户身份信息。',
            features: [
                '/.well-known 端点自动发现',
                'ID Token 验签 (JWKS 公钥库)',
                '标准 UserInfo 声明与读取'
            ],
            link: '/docs/oidc-guide',
        },
        // 新增 SAML 2.0 SP 卡片
        {
            key: 'saml2',
            title: 'SAML 2.0 企业 SSO',
            tag: 'SAML 2.0 (SP)',
            tagColor: 'orange',
            icon: <Building2 size={20} color={token.colorWarning || '#fa8c16'} />,
            description: '作为服务提供者（SP）对接企业级 IdP（Okta, Azure AD 等），支持企业单点登录。',
            features: [
                '提供 SP 元数据 (Metadata) 与 ACS 端点',
                '支持 SAML 断言签名与证书配置',
                '灵活的属性映射 (Email / Roles)'
            ],
            link: '/docs/saml2-guide',
        },
    ];

    return (
        <Card
            title={
                <Flex align="center" gap={8}>
                    <ShieldCheck size={18} color={token.colorPrimary} />
                    <Text strong>接入指引</Text>
                </Flex>
            }
            extra={
                <Button
                    type="link"
                    size="small"
                    onClick={() => navigate('/docs')}
                    style={{ padding: 0 }}
                >
                    完整文档中心 <ExternalLink size={12} />
                </Button>
            }
            variant="borderless"
        >
            <Flex vertical gap={16}>
                {/* 调整栅格：手机 1 列 (24)，平板 2 列 (12)，电脑 3 列 (8) */}
                <Row gutter={[16, 16]}>
                    {docsList.map((doc) => (
                        <Col xs={24} md={12} lg={8} key={doc.key}>
                            <div
                                onClick={() => navigate(doc.link)}
                                style={{
                                    padding: '16px',
                                    borderRadius: token.borderRadiusLG,
                                    border: `1px solid ${token.colorBorderSecondary}`,
                                    backgroundColor: token.colorBgContainer,
                                    cursor: 'pointer',
                                    transition: 'all 0.2s ease',
                                    height: '100%',
                                    display: 'flex',
                                    flexDirection: 'column',
                                    justifyContent: 'space-between',
                                }}
                                className="hover-card-item"
                            >
                                <Flex vertical gap={12}>
                                    {/* 卡片头部 */}
                                    <Flex justify="space-between" align="center">
                                        <Flex align="center" gap={10}>
                                            <div
                                                style={{
                                                    padding: 8,
                                                    borderRadius: token.borderRadius,
                                                    backgroundColor: token.colorFillQuaternary,
                                                    display: 'flex',
                                                }}
                                            >
                                                {doc.icon}
                                            </div>
                                            <Text strong style={{ fontSize: 14 }}>
                                                {doc.title}
                                            </Text>
                                        </Flex>
                                        <Tag color={doc.tagColor} bordered={false}>
                                            {doc.tag}
                                        </Tag>
                                    </Flex>

                                    {/* 描述文字 */}
                                    <Text type="secondary" style={{ fontSize: 12, lineHeight: 1.5 }}>
                                        {doc.description}
                                    </Text>

                                    {/* 特性列表 */}
                                    <Flex vertical gap={6} style={{ marginTop: 4 }}>
                                        {doc.features.map((item, idx) => (
                                            <Flex align="center" gap={6} key={idx}>
                                                <CheckCircle2 size={13} color={token.colorSuccess} style={{ flexShrink: 0 }} />
                                                <Text type="secondary" style={{ fontSize: 12 }}>
                                                    {item}
                                                </Text>
                                            </Flex>
                                        ))}
                                    </Flex>
                                </Flex>

                                {/* 卡片底部跳转提示 */}
                                <Flex align="center" justify="flex-end" gap={4} style={{ marginTop: 16 }}>
                                    <Text strong style={{ fontSize: 12, color: token.colorPrimary }}>
                                        阅读接入文档
                                    </Text>
                                    <ArrowRight size={14} color={token.colorPrimary} />
                                </Flex>
                            </div>
                        </Col>
                    ))}
                </Row>
            </Flex>
        </Card>
    );
};

export default IntegrationGuideCard;