import { Dropdown, Flex, theme, Tooltip } from 'antd';
import './index.css'
import { Check, Palette } from 'lucide-react';
import IconBox from '../../../components/icon-box';
import { useMemo } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { switchColorPrimary } from '../../../redux/slices/layoutSlice';

const ThemeColor = () => {

    const colorPrimary = useSelector(state => state.layout.colorPrimary)

    const dispatch = useDispatch()

    const { token } = theme.useToken()

    const colorOptions = [
        { color: '#1DA57A', label: '晨露' },
        { color: '#2D9CDB', label: '拂晓' },
        { color: '#D94F4F', label: '薄暮' },
        { color: '#FA5418', label: '火山' },
        { color: '#FAAD14', label: '日暮' },
        { color: '#4DB6B6', label: '静谧' },
        { color: '#A85DBF', label: '流光' },
    ]

    const switchColor = (color) => {
        dispatch(switchColorPrimary({ colorPrimary: color }))
    }

    const colorItems = useMemo(() => colorOptions.map(option => ({
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