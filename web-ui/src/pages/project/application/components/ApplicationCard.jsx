import { Avatar, Card, Descriptions, Flex, Popover, Space, Tag, Typography, theme } from "antd"
import {
    AppstoreOutlined,
    InfoCircleOutlined,
    SafetyCertificateOutlined,
    UserOutlined
} from '@ant-design/icons';
import { Egg } from "lucide-react";

const { useToken } = theme;

const { Text, Link } = Typography;

const ApplicationCard = ({ application }) => {


    const { token } = useToken();

    return (
        <Card
            title={
                <Space>
                    所属应用
                </Space>
            }
            variant='borderless'
        >
            <Flex
                align="center"
                gap={14}
                style={{
                    padding: token.paddingSM,
                    marginBottom: token.marginMD,
                    background: token.colorFillAlter,
                    borderRadius: token.borderRadiusLG,
                }}
            >
                <Avatar
                    size={48}
                    shape="square"
                    src={application?.logoUrl}
                    style={{
                        flexShrink: 0,
                        borderRadius: token.borderRadiusLG,
                        color: token.colorPrimary,
                        fontSize: token.fontSizeLG,
                    }}
                />

                <Flex
                    vertical
                    justify="center"
                    gap={token.marginXXS}
                    style={{
                        flex: 1,
                        minWidth: 0,
                    }}
                >
                    <Flex
                        align="center"
                        justify="space-between"
                        gap={token.marginSM}
                    >
                        <Flex align="center" gap={8}>
                            <Text
                                strong
                                ellipsis={{ tooltip: application?.applicationName || '-' }}
                                style={{
                                    color: token.colorTextHeading,
                                    fontSize: token.fontSizeLG,
                                    lineHeight: token.lineHeightLG,
                                }}
                            >
                                {application?.applicationName || '-'}
                            </Text>
                            <Tag color={application?.enabled ? 'processing' : 'error'} bordered={false} style={{ margin: 0, fontSize: token.fontSizeSM }}>
                                {application?.enabled ? '启用' : '停用'}
                            </Tag>
                        </Flex>


                        <Popover
                            trigger="click"
                            placement="bottomLeft"
                            styles={{
                                body: {
                                    borderRadius: token.borderRadiusLG,
                                    padding: token.paddingMD,
                                    maxWidth: 360,
                                },
                            }}
                            content={
                                <Descriptions
                                    column={1}
                                    size="small"
                                    styles={{
                                        label: {
                                            width: 100,
                                            color: token.colorTextSecondary,
                                        },
                                        content: {
                                            minWidth: 0,
                                        },
                                    }}
                                >
                                    <Descriptions.Item label="开发者">
                                        {application?.developerName || '-'}
                                    </Descriptions.Item>

                                    <Descriptions.Item label="联系邮箱">
                                        {application?.developerEmail ? (
                                            <Text copyable>
                                                {application.developerEmail}
                                            </Text>
                                        ) : (
                                            <Text type="secondary">-</Text>
                                        )}
                                    </Descriptions.Item>
                                </Descriptions>
                            }
                        >
                            <Typography.Link
                                style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: token.marginXXS,
                                    fontSize: token.fontSizeSM,
                                }}
                            >
                                开发者信息
                                <InfoCircleOutlined />
                            </Typography.Link>
                        </Popover>

                    </Flex>

                    <Text
                        type="secondary"
                        ellipsis={{ tooltip: application?.description || '暂无应用描述' }}
                        style={{
                            fontSize: token.fontSizeSM,
                            lineHeight: token.lineHeight,
                        }}
                    >
                        {application?.description || '暂无应用描述'}
                    </Text>

                </Flex>
            </Flex>
            <Descriptions
                column={1}
                size="small"
                styles={{
                    label: {
                        width: 120
                    }
                }}
            >
                <Descriptions.Item label="应用主页">
                    {application?.homePageUrl ? (
                        <Link
                            href={application.homePageUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            {application.homePageUrl}
                        </Link>
                    ) : (
                        <Text>-</Text>
                    )}
                </Descriptions.Item>
                <Descriptions.Item label="最新修改">
                    {application?.updateTime ? (
                        <Text>
                            {application.updateTime}
                        </Text>
                    ) : (
                        <Text>-</Text>
                    )}
                </Descriptions.Item>
            </Descriptions>
        </Card>
    )
}

export default ApplicationCard