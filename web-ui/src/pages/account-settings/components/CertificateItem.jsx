
import React, { useState } from 'react';
import {
    Button,
    Typography,
    Flex,
    App,
    theme,
    List,
    Tag,
    Popconfirm,
    Empty,
    Modal,
    Alert,
    Space,
    Radio,
    Card,
    Upload
} from 'antd';
import {
    Plus,
    Trash2,
    Key,
    ChevronDown,
    ShieldAlert,
    FileKey,
    KeyRound,
    Info,
    RefreshCw,
    UploadIcon
} from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import SecurityStepVerify from './verifiers/SecurityStepVerify';

const { Text, Title, Paragraph } = Typography;

/**
 * 证书状态展示
 * status 对应后端 CertificateStatusEnum
 */
const getCertificateStatus = (item) => {
    const status = item.status?.value ?? item.status;

    switch (status) {
        case 'EXPIRING':
            return {
                text: `即将过期 · 剩余 ${item.expireDays ?? '-'} 天`,
                color: 'warning'
            };

        case 'EXPIRED':
            return {
                text: '已过期',
                color: 'error'
            };

        case 'DISABLED':
            return {
                text: '已停用',
                color: 'default'
            };

        case 'REVOKED':
            return {
                text: '已吊销',
                color: 'error'
            };

        default:
            return null;
    }
};

const CertificateItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();

    const [isExpanded, setIsExpanded] = useState(false);

    const [modalMode, setModalMode] = useState(null); // 'bind' | 'unbind' | null

    const [bindMode, setBindMode] = useState('CSR');

    const [csrFile, setCsrFile] = useState(null);

    const [targetItem, setTargetItem] = useState(null)

    // 后端返回的 UserCertificateVO 列表
    const certificates = context?.certificates ?? [];

    const isBound = certificates.length > 0;

    /**
     * 绑定证书
     */
    const handleBind = (e) => {
        e?.stopPropagation();
        setModalMode('bind');
    };

    const handleBindSubmit = () => {

    }

    /**
     * 移除证书
     */
    const handleUnbind = async (item, e) => {
        e?.stopPropagation();
        setTargetItem(item);
        setModalMode('unbind');
    };

    const handleUnbindSubmit = () => {

    }

    const handleCloseModal = () => {
        setModalMode(null);
        setTargetItem(null);
    };

    const isBindLoading = false

    return (
        <Flex
            vertical
            style={{
                borderBottom: `1px solid ${token.colorBorderSecondary}`,
                padding: '20px 0'
            }}
        >
            {/* 主控制行 */}
            <Flex
                justify="space-between"
                align="center"
                onClick={() => isBound && setIsExpanded(!isExpanded)}
                style={{
                    cursor: isBound ? 'pointer' : 'default'
                }}
            >
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div
                        style={{
                            padding: 12,
                            background: token.colorFillAlter,
                            borderRadius: '50%',
                            display: 'flex'
                        }}
                    >
                        <Key
                            style={{
                                width: 20,
                                height: 20,
                                color: token.colorPrimary
                            }}
                        />
                    </div>

                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>
                            数字证书
                        </Text>

                        <Text
                            style={{
                                color: token.colorTextDescription,
                                fontSize: 14,
                                lineHeight: '22px'
                            }}
                        >
                            {isBound
                                ? `已绑定 ${certificates.length} 张数字证书，可用于身份认证及安全访问。`
                                : '尚未绑定数字证书，绑定后可使用数字证书进行身份认证。'
                            }
                        </Text>
                    </Flex>
                </Flex>

                <Flex
                    align="center"
                    gap={12}
                    onClick={e => e.stopPropagation()}
                >
                    <Button
                        type={isBound ? 'default' : 'primary'}
                        onClick={handleBind}
                    >
                        {isBound ? '添加证书' : '立即添加'}
                    </Button>

                    {isBound && (
                        <div
                            onClick={() => setIsExpanded(!isExpanded)}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                padding: 4,
                                cursor: 'pointer'
                            }}
                        >
                            <ChevronDown
                                size={18}
                                style={{
                                    color: token.colorTextDescription,
                                    transform: isExpanded
                                        ? 'rotate(180deg)'
                                        : 'rotate(0deg)',
                                    transition: 'transform 0.2s'
                                }}
                            />
                        </div>
                    )}
                </Flex>
            </Flex>

            {/* 证书列表 */}
            <AnimatePresence>
                {isBound && isExpanded && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{
                            duration: 0.2,
                            ease: 'easeInOut'
                        }}
                        style={{
                            position: 'relative',
                            paddingLeft: 22,
                            overflow: 'hidden'
                        }}
                    >
                        {/* 垂直连接线 */}
                        <div
                            style={{
                                position: 'absolute',
                                left: 21,
                                top: -10,
                                bottom: 24,
                                width: 1.5,
                                backgroundColor: token.colorBorderSecondary,
                                zIndex: 1
                            }}
                        />

                        <List
                            itemLayout="horizontal"
                            dataSource={certificates}
                            rowKey="id"
                            split={false}
                            style={{ marginTop: 8 }}
                            renderItem={(item) => {
                                const status = getCertificateStatus(item);

                                return (
                                    <List.Item
                                        style={{
                                            padding: '12px 0 12px 24px',
                                            position: 'relative'
                                        }}
                                    >
                                        {/* 横向连接线 */}
                                        <div
                                            style={{
                                                position: 'absolute',
                                                left: 0,
                                                top: 28,
                                                width: 12,
                                                height: 1.5,
                                                backgroundColor: token.colorBorderSecondary,
                                                zIndex: 1
                                            }}
                                        />

                                        <List.Item.Meta
                                            avatar={
                                                <div
                                                    style={{
                                                        padding: 6,
                                                        background: token.colorFillAlter,
                                                        borderRadius: '50%',
                                                        display: 'flex',
                                                        border: `1px solid ${token.colorBorderSecondary}`,
                                                        zIndex: 2
                                                    }}
                                                >
                                                    <Key
                                                        style={{
                                                            width: 13,
                                                            height: 13,
                                                            color: token.colorTextDescription
                                                        }}
                                                    />
                                                </div>
                                            }
                                            title={
                                                <Flex align="center" gap={8} wrap="wrap">
                                                    <Text
                                                        style={{
                                                            fontSize: 14,
                                                            fontWeight: 500
                                                        }}
                                                    >
                                                        {item.label || '未命名证书'}
                                                    </Text>

                                                    {status && (
                                                        <Tag
                                                            color={status.color}
                                                            style={{
                                                                marginInlineEnd: 0,
                                                                fontSize: 11
                                                            }}
                                                        >
                                                            {status.text}
                                                        </Tag>
                                                    )}
                                                </Flex>
                                            }
                                            description={
                                                <Text
                                                    type="secondary"
                                                    style={{ fontSize: 12 }}
                                                >
                                                    有效期至：{item.notAfter || '-'}
                                                </Text>
                                            }
                                        />
                                        <Button
                                            type="text" danger size="small"
                                            icon={<Trash2 style={{ width: 14, height: 14 }} />}
                                            onClick={(e) => handleUnbind(item, e)}
                                            style={{ display: 'flex', alignItems: 'center', gap: 4 }}
                                        >
                                            移除
                                        </Button>
                                    </List.Item>
                                );
                            }}
                        />
                    </motion.div>
                )}
            </AnimatePresence>

            <Modal
                title={
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {modalMode === 'unbind' ? '移除数字证书' : '绑定数字证书'}
                        </Title>
                    </div>
                }
                open={modalMode !== null}
                onCancel={handleCloseModal}
                width={460}
                centered
                destroyOnHidden
                footer={null}
            >
                {modalMode === 'bind' && (
                    <SecurityStepVerify
                        scene="BIND_CERTIFICATE" // 后端风控定义对应的场景标识
                        captchaScene="default"
                        context={context}
                        stepTitle="添加数字证书"
                        confirmText={bindMode === 'CSR' ? '提交申请' : '生成并签发'}
                        confirmLoading={isBindLoading}
                        onCancel={handleCloseModal}
                        onConfirm={handleBindSubmit}
                    >
                        <Flex vertical gap={12} style={{ padding: '4px 0 16px 0' }}>
                            {bindMode === 'CSR' ? (
                                <>
                                    <Flex align="center" gap={8}>
                                        <FileKey size={18} />
                                        <Text strong>提交 CSR 申请证书</Text>
                                        <Tag color="success">推荐</Tag>
                                    </Flex>

                                    <Paragraph type="secondary" style={{ fontSize: 13, margin: 0 }}>
                                        在本地生成密钥对和证书签名请求（CSR），上传后由 Atlas CA 为您签发数字证书。
                                    </Paragraph>

                                    <Upload
                                        accept=".csr,.pem,.txt"
                                        maxCount={1}
                                        beforeUpload={(file) => {
                                            setCsrFile(file);
                                            return false;
                                        }}
                                        onRemove={() => setCsrFile(null)}
                                        fileList={csrFile ? [{
                                            uid: csrFile.uid,
                                            name: csrFile.name,
                                            status: 'done'
                                        }] : []}
                                    >
                                        <Button icon={<UploadIcon size={15} />} block>
                                            上传 CSR 文件
                                        </Button>
                                    </Upload>

                                    <Text type="secondary" style={{ fontSize: 12 }}>
                                        支持 PEM 格式的 CSR 文件，请确保对应私钥由您自行保管。
                                    </Text>
                                </>
                            ) : (
                                <>
                                    <Flex align="center" gap={8}>
                                        <KeyRound size={18} />
                                        <Text strong>由 Atlas 生成密钥</Text>
                                    </Flex>

                                    <Alert
                                        type="info"
                                        showIcon
                                        message="由 Atlas 自动生成密钥对并签发证书"
                                        description="Atlas 将生成密钥对并完成证书签发，随后提供安全方式获取证书及私钥。请妥善保管私钥。"
                                    />
                                </>
                            )}

                            <div
                                style={{
                                    borderTop: `1px solid ${token.colorBorderSecondary}`,
                                    paddingTop: 12
                                }}
                            >
                                <Button
                                    type="link"
                                    size="small"
                                    icon={<RefreshCw size={14} />}
                                    style={{ padding: 0 }}
                                    onClick={() => {
                                        setBindMode(bindMode === 'CSR' ? 'ATLAS' : 'CSR');
                                        setCsrFile(null);
                                    }}
                                >
                                    {bindMode === 'CSR'
                                        ? '不想自己生成密钥？改用 Atlas 生成'
                                        : '改为提交 CSR 申请'}
                                </Button>
                            </div>
                        </Flex>
                    </SecurityStepVerify>
                )}

                {modalMode === 'unbind' && (
                    <SecurityStepVerify
                        scene="UNBIND_CERTIFICATE"
                        captchaScene="default"
                        context={context}
                        stepTitle="风险确认"
                        confirmText="确定移除"
                        confirmDanger={true}
                        confirmLoading={false}
                        onCancel={handleCloseModal}
                        onConfirm={handleUnbindSubmit}
                    >
                        <Flex vertical gap={14}>
                            <Alert
                                message="数字证书移除风险提示"
                                description="移除后，该证书将解除与当前账号的关联，您将无法再使用该证书进行身份认证。如需再次使用，您需要重新添加该证书。此操作不会吊销证书本身。"
                                type="warning"
                                showIcon
                                icon={<ShieldAlert style={{ color: token.colorWarning }} />}
                            />
                            <div style={{ padding: '4px 12px', background: token.colorFillAlter, borderRadius: 8, border: `1px solid ${token.colorBorderSecondary}` }}>
                                <Space direction="vertical" size={2} style={{ padding: '8px 0' }}>
                                    <Text type="secondary" style={{ fontSize: 13 }}>即将解除关联的证书：</Text>
                                    <Text strong style={{ fontSize: 15, color: token.colorText }}>{targetItem?.label || '未命名证书'}</Text>
                                    <Text type="secondary" style={{ fontSize: 12 }}> 有效期至：{targetItem?.notAfter || '-'}</Text>
                                </Space>
                            </div>
                            <Text type="danger" style={{ fontSize: 13, fontWeight: 500 }}>
                                💡 确认无误请在完成上方风控后，点击下方的 “确定移除”。
                            </Text>
                        </Flex>
                    </SecurityStepVerify>
                )}
            </Modal>
        </Flex>
    );
};

export default CertificateItem;
