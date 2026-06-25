import { useRef, useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Flex, Tooltip } from 'antd';
import { GithubOutlined, GoogleOutlined, AlipayCircleOutlined } from '@ant-design/icons';
import { AtlasLogo, Auth0Icon } from '../../../../components/icons';
import { useRequest } from 'ahooks';
import { AUTHORIZE_CODE_PKCE_VERIFIER, fetchAuthorizeUrl } from '../../../../services/Oauth2Service';
import { generateChallenge, generateVerifier } from '../../../../utils/pkce';

const ThirdPartyLogin = () => {
    const constraintsRef = useRef(null);
    const containerRef = useRef(null);
    const [dragConstraints, setDragConstraints] = useState({ left: 0, right: 0 });

    const { runAsync: getAuthorizeUrlAsync, loading: getAuthorizeUrlLoading } = useRequest(fetchAuthorizeUrl, {
        manual: true
    });

    const ICON_SIZE = 24;
    const GAP = 28;
    const PADDING_X = 8;

    // 🚀 核心 UI 修改 1：将展示数量从 3 改为 3.4
    // 这样第 4 个图标会露出一半左右（3.4个图标宽 + 3个间距 + 左右内边距）
    const DISPLAY_COUNT = 3.4;
    const visibleWidth = `${(ICON_SIZE * DISPLAY_COUNT) + (GAP * Math.floor(DISPLAY_COUNT)) + (PADDING_X * 2)}px`;

    useEffect(() => {
        if (containerRef.current && constraintsRef.current) {
            const containerWidth = constraintsRef.current.offsetWidth;
            const contentWidth = containerRef.current.scrollWidth;

            setDragConstraints({
                left: containerWidth - contentWidth,
                right: 0
            });
        }
    }, []);

    const authorizeCodeLogin = async (clientName, protocol = 'OAUTH2') => {
        const { authorizeUrl, isPKCERequired } = await getAuthorizeUrlAsync(clientName, protocol);
        let finalUrl = authorizeUrl
        if (isPKCERequired) {
            const verifier = generateVerifier()
            const challenge = await generateChallenge(verifier)
            sessionStorage.setItem(AUTHORIZE_CODE_PKCE_VERIFIER, verifier)
            finalUrl = finalUrl + `&code_challenge=${challenge}&code_challenge_method=S256`
        }
        window.location.href = finalUrl;
    };

    const iconHoverAnimation = {
        whileHover: { translateY: -2 },
        transition: { duration: 0.2, ease: "easeInOut" }
    };

    return (
        <div style={{ position: 'relative', width: visibleWidth, }}>

            {/* 1. 外层包裹视口 */}
            <div
                ref={constraintsRef}
                style={{
                    width: '100%',
                    overflow: 'hidden',
                    cursor: 'grab'
                }}
            >
                {/* 2. 运动容器 */}
                <motion.div
                    ref={containerRef}
                    drag="x"
                    dragConstraints={dragConstraints}
                    dragElastic={0.1}
                    whileTap={{ cursor: 'grabbing' }}
                    style={{ display: 'inline-block', whiteSpace: 'nowrap' }}
                >
                    {/* 3. 内部 Flex 布局 */}
                    <Flex
                        align="center"
                        gap={GAP}
                        style={{ padding: `4px ${PADDING_X}px` }}
                    >
                        <Tooltip title='GitHub'>
                            <motion.div {...iconHoverAnimation} style={{ width: ICON_SIZE, display: 'flex', justifyContent: 'center' }}>
                                <GithubOutlined
                                    style={{ fontSize: 22, color: '#111827', cursor: 'pointer', transition: 'all 0.2s ease' }}
                                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-2px)'; }}
                                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; }}
                                    onClick={() => authorizeCodeLogin('gitHub')}
                                />
                            </motion.div>
                        </Tooltip>

                        <Tooltip title='Atlas'>
                            <motion.div {...iconHoverAnimation} style={{ width: ICON_SIZE, display: 'flex', justifyContent: 'center' }}>
                                <AtlasLogo
                                    style={{
                                        width: '24px',
                                        height: '24px',
                                        cursor: 'pointer',
                                        transition: 'all 0.2s ease',
                                        background: 'transparent',
                                    }}
                                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-2px)'; }}
                                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; }}
                                    onClick={() => authorizeCodeLogin('atlas', 'OIDC')}
                                />
                            </motion.div>
                        </Tooltip>

                        <Tooltip title='Google'>
                            <motion.div {...iconHoverAnimation} style={{ width: ICON_SIZE, display: 'flex', justifyContent: 'center' }}>
                                <GoogleOutlined
                                    style={{ fontSize: 22, color: '#EA4335', cursor: 'pointer', transition: 'all 0.2s ease' }}
                                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-2px)'; }}
                                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; }}
                                    onClick={() => authorizeCodeLogin('google', 'OIDC')}
                                    title="Google"
                                />
                            </motion.div>
                        </Tooltip>


                        <Tooltip title='支付宝'>
                            <motion.div {...iconHoverAnimation} style={{ width: ICON_SIZE, display: 'flex', justifyContent: 'center' }}>
                                <AlipayCircleOutlined
                                    style={{ fontSize: 24, color: '#1677FF', cursor: 'pointer', transition: 'all 0.2s ease' }}
                                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-2px)'; }}
                                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; }}
                                    onClick={() => authorizeCodeLogin('alipay', 'OAUTH2')}
                                />
                            </motion.div>
                        </Tooltip>

                        <Tooltip title='Auth0'>
                            <motion.div {...iconHoverAnimation} style={{ width: ICON_SIZE, display: 'flex', justifyContent: 'center' }}>
                                <Auth0Icon
                                    style={{
                                        width: '22px',
                                        height: '22px',
                                        cursor: 'pointer',
                                        transition: 'all 0.2s ease',
                                    }}
                                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-2px)'; }}
                                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; }}
                                    onClick={() => authorizeCodeLogin('auth0', 'SAML2')}
                                />
                            </motion.div>
                        </Tooltip>

                        <div style={{ width: '5px', flexShrink: 0 }} />

                    </Flex>
                </motion.div>
            </div>

            <div
                style={{
                    position: 'absolute',
                    top: 0,
                    right: 0,
                    bottom: 0,
                    width: '24px',
                    background: 'linear-gradient(to right, transparent, #ffffff)',
                    pointerEvents: 'none'
                }}
            />
        </div>
    );
}

export default ThirdPartyLogin;