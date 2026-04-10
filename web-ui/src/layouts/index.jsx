import { Affix, App, ConfigProvider, Flex, Layout, theme, Watermark } from 'antd';
import { Suspense, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useSelector, useDispatch } from 'react-redux';
import { useLocation, useNavigate, useOutlet } from 'react-router-dom';
import Loading from '../components/loading';
import ServerError from '../pages/ServerError';
import Header from './header';
import './index.css';
import Sider from './sider';
import TopMenuTab from './top-tab';
import { SwitchTransition, CSSTransition } from 'react-transition-group';
import { setUserInfo } from '../redux/slices/userSlice';
import { setAuthInfo } from '../redux/slices/authSlice';
import { loadMenuItems } from '../redux/slices/layoutSlice';
import { fetchAuthInfo, fetchUserInfo } from '../services/UserProfileService';
import { useAuth } from '../router/AuthProvider';
import { SseProvider } from '../sse/SseProvider';
import FullScreenButton from '../components/FullScreenButton';


const { Header: LayoutHeader, Content: LayoutContent, Sider: LayoutSider } = Layout;

const AppLayout = () => {

    const outlet = useOutlet()

    const nodeRef = useRef(null)

    const mainDivRef = useRef(null)

    const collapsed = useSelector(state => state.layout.menuCollapsed)

    const themeValue = useSelector(state => state.layout.theme)

    const redirectTo = useSelector(state => state.layout.redirectTo)

    const { accessToken } = useAuth()

    const SSE_URL = useMemo(() => {
        if (!accessToken) {
            return null
        }
        return `/api/notification/v1/notification/sse/subscribe?terminal=web&access_token=${accessToken}`
    }, [accessToken])

    const location = useLocation()
    const navigate = useNavigate()

    const dispatch = useDispatch()

    const [loading, setLoading] = useState(true)

    useLayoutEffect(() => {
        const fetchData = async () => {
            try {
                const userInfo = await fetchUserInfo()
                const authInfo = await fetchAuthInfo()
                dispatch(setUserInfo({ userInfo }))
                dispatch(setAuthInfo({ authInfo }))
                dispatch(loadMenuItems({ menuItems: authInfo.menus }))
            } finally {
                setLoading(false)
            }

        }
        fetchData()
    }, [])

    useEffect(() => {
        if (redirectTo) {
            navigate(redirectTo)
        }
    }, [redirectTo])

    const {
        token: {
            colorBgContainer,
            borderRadius
        }
    } = theme.useToken()

    const [isFS, setIsFS] = useState(false)

    const watermarkColor = useMemo(() => {
        return themeValue === 'dark'
            ? 'rgba(255, 255, 255, 0.05)'  // 暗色模式：白色半透明
            : 'rgba(0, 0, 0, 0.08)'       // 亮色模式：黑色半透明
    }, [themeValue])

    if (loading) {
        return <Flex justify='center' align='center' style={{ width: '100vw', height: '100vh' }}><Loading fullscreen /></Flex>
    }

    return (
        <ConfigProvider
            getPopupContainer={(node) => {
                // 如果已经在全屏状态，挂载到 main-div
                if (document.fullscreenElement) {
                    return mainDivRef.current || document.body
                }
                return document.body;
            }}
        >
            <App
                notification={{
                    threshold: 3,
                    placement: 'topRight',
                    duration: 5,
                    top: 109,
                }}
            >
                <SseProvider url={SSE_URL}>
                    <Layout style={{ minHeight: '100vh' }}>
                        {/* 侧边菜单 */}
                        <LayoutSider
                            width='240px'
                            theme={themeValue}
                            collapsible
                            collapsed={collapsed}
                            trigger={null}
                        >
                            <Sider />
                        </LayoutSider>
                        <Layout>
                            {/* 头部 */}
                            <LayoutHeader className='layout-header'
                                style={{
                                    boxShadow: themeValue === 'dark' ? '0 1px 4px rgba(0, 0, 0, 0.45)' : '0 2px 4px rgba(0, 0, 0, 0.06)'
                                }}
                            >
                                <Header />
                            </LayoutHeader>
                            {/* 主题内容 */}
                            <LayoutContent style={{ margin: '0 16px', height: 'auto', overflow: 'initial', scrollbarGutter: 'stable' }}>
                                {/* 顶部页签 */}
                                <TopMenuTab style={{ height: '45px', width: '100%' }} />
                                <div
                                    ref={mainDivRef}
                                    className='main-div'
                                    style={{
                                        position: 'relative',
                                        height: isFS ? '100vh' : 'calc(100vh - 109px)',
                                        width: '100%',
                                        overflow: 'auto',
                                        padding: 20,
                                        borderRadius: borderRadius,
                                        background: colorBgContainer,
                                    }}
                                >
                                    <Affix
                                        target={() => mainDivRef.current}
                                        offsetTop={0}
                                        style={{ position: 'absolute', top: 0, right: 0, zIndex: 9999 }}
                                    >
                                        <FullScreenButton targetRef={mainDivRef} onStateChange={(state) => setIsFS(state)} />
                                    </Affix>
                                    <ErrorBoundary
                                        fallback={<ServerError />}
                                        resetKeys={[location.pathname]}
                                    >

                                        <SwitchTransition mode="out-in">
                                            <CSSTransition
                                                key={location.pathname}
                                                nodeRef={nodeRef}
                                                appear={true}
                                                timeout={300}
                                                classNames="page"
                                                unmountOnExit
                                            >
                                                <Suspense
                                                    fallback={
                                                        <Flex style={{ height: '100%' }} justify='center' align='center'>
                                                            <Loading />
                                                        </Flex>
                                                    }
                                                >
                                                    <div
                                                        style={{
                                                            width: '100%',
                                                            margin: isFS ? 'auto 0' : 'unset',
                                                            height: isFS ? 'auto' : '100%',
                                                            flexShrink: 0,
                                                        }}
                                                        ref={nodeRef}
                                                    >
                                                        <Watermark
                                                            content="Atlas"
                                                            gap={[120, 120]}
                                                            font={{ color: watermarkColor }}
                                                            style={{ width: '100%' }}
                                                        >
                                                            {outlet}
                                                        </Watermark>
                                                    </div>
                                                </Suspense>
                                            </CSSTransition>
                                        </SwitchTransition>
                                    </ErrorBoundary>
                                </div>
                            </LayoutContent>
                        </Layout>
                    </Layout >
                </SseProvider>
            </App>
        </ConfigProvider>
    )
}

export default AppLayout