import React from 'react';
import { Dropdown, Flex, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import './index.css'
import { Languages } from 'lucide-react';
import { useSelector, useDispatch } from 'react-redux';
import IconBox from '../../../components/icon-box';
import { switchLanguage } from '../../../redux/slices/layoutSlice';

const LanguageSwitch = () => {

    const language = useSelector(state => state.layout.language)

    const dispatch = useDispatch()

    const { i18n } = useTranslation()

    const changeLanguage = (lng) => {
        dispatch(switchLanguage({ language: lng }))
        i18n.changeLanguage(lng)
    }

    const languageOptions = [
        { language: 'zh', label: '简体中文' },
        { language: 'en', label: 'English' }
    ]

    const languageItems = languageOptions.map(option => ({
        key: option.language,
        label: (
            <Typography.Link onClick={() => changeLanguage(option.language)}>
                {option.label}
            </Typography.Link>
        ),
    }))

    return (
        <Dropdown
            menu={{
                selectedKeys: [language],
                selectable: true,
                items: languageItems,
            }}
            placement="bottom"
        >
            <Flex>
                <IconBox>
                    <Languages size={20} />
                </IconBox>
            </Flex>
        </Dropdown >
    )

}

export default LanguageSwitch