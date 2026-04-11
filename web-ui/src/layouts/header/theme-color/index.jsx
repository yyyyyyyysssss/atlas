import { Dropdown, Flex, theme, Tooltip } from 'antd';
import './index.css'
import { Check, Palette } from 'lucide-react';
import IconBox from '../../../components/icon-box';
import { useMemo } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { switchColorPrimary } from '../../../redux/slices/userSlice';
import { changeAppearance } from '../../../services/UserProfileService';
import { useRequest } from 'ahooks';

const COLOR_PRIMARY_OPTIONS = [
    { color: '#1DA57A', label: '晨露' },
    { color: '#2D9CDB', label: '拂晓' },
    { color: '#D94F4F', label: '薄暮' },
    { color: '#FA5418', label: '火山' },
    { color: '#FAAD14', label: '日暮' },
    { color: '#4DB6B6', label: '静谧' },
    { color: '#A85DBF', label: '流光' },
]

export const DEFAULT_PRIMARY_COLOR = COLOR_PRIMARY_OPTIONS[0].color

const ThemeColor = () => {

    const colorPrimary = useSelector(state => state.user.userInfo?.settings?.appearance?.colorPrimary || DEFAULT_PRIMARY_COLOR)

    const dispatch = useDispatch()

    const { token } = theme.useToken()

    const { runAsync: changeAppearanceAsync, loading: changeAppearanceLoading } = useRequest(changeAppearance, {
        manual: true
    })



    const switchColor = (color) => {
        dispatch(switchColorPrimary({ colorPrimary: color }))
        changeAppearanceAsync({
            colorPrimary: color
        })
    }

    const colorItems = useMemo(() => COLOR_PRIMARY_OPTIONS.map(option => ({
        key: option.color,
        label: (
            <Tooltip title={option.label}>
                <div
                    onClick={() => switchColor(option.color)}
                    style={{
                        position: 'relative',
                        backgroundColor: option.color,
                        borderRadius: token.borderRadius,
                        width: '25px',
                        height: '25px'
                    }}
                >
                    {colorPrimary === option.color && (
                        <Check
                            size={22}
                            style={{
                                position: 'absolute',
                                top: '2px',
                                right: '2px',
                                bottom: '2px',
                                left: '2px',
                                color: '#fff',
                                fontSize: '8px',
                            }}
                        />
                    )}
                </div>
            </Tooltip>
        ),
    })), [colorPrimary])

    return (
        <Dropdown
            menu={{
                items: colorItems
            }}
            placement="bottom"
        >
            <Flex>
                <IconBox>
                    <Palette size={20} />
                </IconBox>
            </Flex>
        </Dropdown >
    )
}

export default ThemeColor