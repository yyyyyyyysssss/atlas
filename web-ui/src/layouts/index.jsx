import { Flex, Layout, theme } from 'antd';
import { Suspense, useEffect, useLayoutEffect, useRef, useState } from 'react';
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


const { Header: LayoutHeader, Content: LayoutContent, Sider: LayoutSider } = Layout;

const AppLayout = () => {

    const outlet = useOutlet()

    const nodeRef = useRef(null)

    const collapsed = useSelector(state => state.layout.menuCollapsed)

    const themeValue = useSelector(state => state.layout.theme)

    const redirectTo = useSelector(state => state.layout.redirectTo)

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

    if (loading) {
        return <Flex justify='center' align='center' style={{ width: '100vw', height: '100vh' }}><Loading fullscreen /></Flex>
    }

    return (
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
                        className='main-div'
                        style={{
                            height: 'calc(100vh - 109px)',
                            width: '100%',
                            overflow: 'auto',
                            padding: 20,
                            borderRadius: borderRadius,
                            background: colorBgContainer
                        }}
                    >
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
                                        <div style={{ height: '100%', width: '100%'}} ref={nodeRef}>
                                            {outlet}
                                        </div>
                                    </Suspense>
                                </CSSTransition>
                            </SwitchTransition>
                        </ErrorBoundary>
                    </div>
                </LayoutContent>
            </Layout>
        </Layout >
    )
}

export default AppLayout