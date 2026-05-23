import React from 'react';
import { Button, Typography, Flex, App, theme, Space } from 'antd';
import { Fingerprint } from 'lucide-react';
import { useRequest } from 'ahooks';
// 💡 确保这里引入了你后端对应的三个真实接口
import {
    getWebauthnRegisterOptions,
    webauthnRegister,
} from '../../../services/UserProfileService';

const { Text } = Typography;


const getDeviceLabel = () => {
    const ua = navigator.userAgent;
    let os = "未知设备";
    let browser = "浏览器";

    // 1. 精准识别系统
    if (ua.indexOf("Win") !== -1) os = "Windows";
    else if (ua.indexOf("Mac") !== -1) os = "Mac";
    else if (ua.indexOf("X11") !== -1) os = "Linux";
    else if (ua.indexOf("Android") !== -1) os = "Android";
    else if (ua.indexOf("iPhone") !== -1) os = "iPhone/iPad";

    // 2. 精准识别主流浏览器
    if (ua.indexOf("Edg") !== -1) browser = "Edge";
    else if (ua.indexOf("Chrome") !== -1 && ua.indexOf("Safari") !== -1) browser = "Chrome";
    else if (ua.indexOf("Firefox") !== -1) browser = "Firefox";
    else if (ua.indexOf("Safari") !== -1 && ua.indexOf("Chrome") === -1) browser = "Safari";

    return `${os} (${browser})`;
};

const PasskeyItem = ({ passkeyBound, refresh }) => {
    const { token } = theme.useToken();
    const { message, modal } = App.useApp();

    // 💡 检测当前浏览器环境是否全面支持 Passkey 及现代 JSON 序列化标准
    const isWebAuthnSupported =
        window.PublicKeyCredential !== undefined &&
        typeof window.PublicKeyCredential === 'function' &&
        typeof PublicKeyCredential.parseCreationOptionsFromJSON === 'function';

    // 1. 获取注册配置的请求
    const { runAsync: getOptionsAsync, loading: getOptionsLoading } = useRequest(getWebauthnRegisterOptions, {
        manual: true
    });

    // 2. 确认绑定的请求
    const { runAsync: webauthnRegisterAsync, loading: webauthnRegisterLoading } = useRequest(webauthnRegister, {
        manual: true
    });

    // 动态计算指纹图标颜色
    const getIconColor = () => {
        if (passkeyBound) return token.colorSuccess;
        if (isWebAuthnSupported) return token.colorPrimary;
        return token.colorTextDisabled;
    };

    // 🎯 核心逻辑一：执行绑定流程（纯原生 W3C 标准）
    const handleBind = async () => {
        try {
            // 从后端获取包含 Base64URL 字符串的 WebAuthn 配置
            const registerOptions = await getOptionsAsync();
            message.loading({ content: '正在唤起设备凭证...', key: 'passkey_action' });

            // 现代浏览器原生方法，自动处理 Base64URL -> ArrayBuffer 的复杂转换
            const nativeOptions = PublicKeyCredential.parseCreationOptionsFromJSON(registerOptions);

            // 唤起操作系统原生的生物识别（指纹、面容或安全密钥）
            const credential = await navigator.credentials.create({
                publicKey: nativeOptions
            });
            message.loading({ content: '正在向服务器验证...', key: 'passkey_action' });

            const credentialJson = credential.toJSON()

            const label = getDeviceLabel()
            // 发送给服务器做最终的签名验证并落库
            await webauthnRegisterAsync({
                publicKey: {
                    label: label, 
                    credential: credentialJson // 把浏览器原始结构塞进 credential 里
                }
            });

            message.success({ content: '通行密钥绑定成功！', key: 'passkey_action' });

            // 刷新父组件，更新绑定状态数据
            if (refresh) refresh()

        } catch (error) {
            console.error('Passkey 绑定失败:', error);

            if (error instanceof DOMException || error.name) {
                switch (error.name) {
                    case 'NotAllowedError':
                        // 用户主动取消、超时未验证、或者拒绝了权限申请
                        message.warning({ content: '操作已取消或验证超时', key: 'passkey_action' });
                        break;

                    case 'InvalidStateError':
                    case 'ConstraintError':
                        // 👈 核心修复：命中后端传递的 excludeCredentials 列表
                        message.info({
                            content: '该设备/验证器已经绑定过此账号，无需重复绑定',
                            key: 'passkey_action',
                            duration: 4
                        });
                        break;

                    case 'NotSupportedError':
                        // 浏览器或当前域名不支持 WebAuthn（例如没走 HTTPS）
                        message.error({ content: '当前环境不支持密钥绑定，请确保通过 HTTPS 访问', key: 'passkey_action' });
                        break;

                    case 'SecurityError':
                        // 域名不匹配（rpId 校验失败）或架构安全拦截
                        message.error({ content: '安全校验未通过，请检查域名配置', key: 'passkey_action' });
                        break;

                    default:
                        // 其他浏览器原生的硬件错误（如指纹连续识别失败导致硬件锁定等）
                        message.error({ content: `设备交互失败: ${error.message || '未知错误'}`, key: 'passkey_action' });
                        break;
                }
            } else {
                // 后端网络请求报错（例如 webauthnRegisterAsync 抛出的业务异常）
                message.error({
                    content: error?.response?.data?.message || error?.message || '服务器验证失败，请稍后重试',
                    key: 'passkey_action'
                });
            }
        }
    };

    // 🎯 核心逻辑二：执行解绑流程
    const handleUnbind = () => {
        modal.confirm({
            title: '确认解除绑定？',
            content: '解绑后您将无法使用指纹、面容或 PIN 码快速登录此设备。',
            okText: '确认解绑',
            cancelText: '取消',
            okButtonProps: { danger: true },
            onOk: async () => {
                try {
                    message.loading({ content: '正在解除绑定...', key: 'passkey_action' });

                    // 调用解绑 API
                    await unbindWebauthnAsync();

                    message.success({ content: '已成功解除绑定', key: 'passkey_action' });

                    if (refresh) refresh();
                } catch (error) {
                    console.error('Passkey 解绑失败:', error);
                    message.error({ content: error.message || '解绑失败，请稍后重试', key: 'passkey_action' });
                }
            }
        });
    };

    // 汇总绑定流程和解绑流程的全局加载状态，用来控制按钮的 Loading 和禁用
    const isGlobalLoading = getOptionsLoading || webauthnRegisterLoading;

    return (
        <Flex
            justify="space-between"
            align="center"
            style={{
                padding: '20px 0',
                borderBottom: `1px solid ${token.colorBorderSecondary}`
            }}
        >
            {/* 左侧：图标 + 标题与当前状态描述 */}
            <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                <div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%', display: 'flex' }}>
                    <Fingerprint style={{ width: 20, height: 20, color: getIconColor() }} />
                </div>
                <Flex vertical gap={4}>
                    <Text strong style={{ fontSize: 16 }}>通行密钥 (Passkey)</Text>
                    <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                        {!isWebAuthnSupported
                            ? '您的当前设备或浏览器版本过低，不支持通行密钥功能。'
                            : (passkeyBound ? '已绑定当前设备。您可以使用指纹、面容或 PIN 快速登录。' : '未绑定。绑定后可实现无密码安全登录。')}
                    </Text>
                </Flex>
            </Flex>

            {/* 右侧：根据绑定状态动态渲染的操作按钮 */}
            <Flex align="center" style={{ justifyContent: 'flex-end' }}>
                {!passkeyBound ? (
                    <Button
                        type="primary"
                        disabled={!isWebAuthnSupported}
                        loading={isGlobalLoading}
                        onClick={handleBind}
                    >
                        立即绑定
                    </Button>
                ) : (
                    <Space size="small">
                        <Button
                            disabled={isGlobalLoading}
                            onClick={() => message.info('管理已绑定设备功能开发中...')}
                        >
                            管理设备
                        </Button>
                        <Button
                            danger
                            type="text"
                            loading={unbindLoading}
                            disabled={isGlobalLoading}
                            onClick={handleUnbind}
                        >
                            解绑
                        </Button>
                    </Space>
                )}
            </Flex>
        </Flex>
    );
};

export default PasskeyItem;