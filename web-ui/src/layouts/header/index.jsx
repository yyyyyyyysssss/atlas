import {
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    MoonOutlined,
    SunOutlined,
} from '@ant-design/icons';
import { Button, Flex, Segmented } from 'antd';
import { useDispatch, useSelector } from 'react-redux';
import { menuCollapsed, switchTheme } from '../../redux/slices/layoutSlice';
import TopBreadcrumbTab from '../breadcrumb-tab';
import './index.css';
import UserProfile from './user-profile';
import SearchMenu from './search-menu';
import ThemeColor from './theme-color';
import LanguageSwitch from './language-switch';


const Header = () => {

    const themeValue = useSelector(state => state.layout.theme)

    const collapsed = useSelector(state => state.layout.menuCollapsed)

    const dispatch = useDispatch()

    const handleCollapsed = () => {
        dispatch(menuCollapsed())
    }

    const handleSwitchTheme = (themeValue) => {
        dispatch(switchTheme({ theme: themeValue }))
    }

    return (
        <Flex justify="space-between" style={{ height: '100%' }}>
            <Flex
                align='center'
            >
                <Button
                    type="text"
                    icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                    onClick={handleCollapsed}
                    style={{
                        fontSize: '16px',
                        width: 48,
                        height: 48,
                    }}
                />
                {/* 面包屑 */}
                <TopBreadcrumbTab />
            </Flex>
            <Flex
                style={{ paddingRight: 10 }}
                align='center'
                justify='center'
                gap={10}
            >
                <SearchMenu/>
                <ThemeColor/>
                <LanguageSwitch/>
                <Segmented
                    size='middle'
                    shape="round"
                    value={themeValue}
                    onChange={handleSwitchTheme}
                    options={[
                        { value: 'light', icon: <SunOutlined /> },
                        { value: 'dark', icon: <MoonOutlined /> },
                    ]}
                />
                <UserProfile />
            </Flex>
        </Flex>
    )
}

export default Header