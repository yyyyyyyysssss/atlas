import { useEffect, useRef, useState } from 'react';
import { AutoComplete, Button, Flex, Input } from "antd"
import {
    SearchOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import './index.css'
import { findRouteByPath } from '../../../router/router';
import IconBox from '../../../components/icon-box';

const SearchMenu = () => {

    const menuItems = useSelector(state => state.layout.flattenMenuItems)

    const navigate = useNavigate()

    const [searchValue, setSearchValue] = useState('')

    const [options, setOptions] = useState([])

    const [isSearching, setIsSearching] = useState(false)

    const inputRef = useRef(null)


    useEffect(() => {
        if (isSearching && inputRef.current) {
            inputRef.current.focus();  // 确保在展开后获取焦点
        }
    }, [isSearching])

    const handleSearch = (value) => {
        setSearchValue(value)
        if (!value) {
            setOptions([])
            return
        }
        // 模糊匹配菜单项
        const filteredOptions = menuItems
            .filter(item => item.name.toLowerCase().includes(value.toLowerCase()))
            .filter(item => {
                const route = findRouteByPath(item.routePath);
                return route && route.element
            })
            .map(item => ({
                value: item.routePath,
                label: item.name,
            }))

        setOptions(filteredOptions)
    }

    const handleSelect = (value) => {
        navigate(value)
        setSearchValue('')
        setOptions([])
    }

    const handleClick = () => {
        setIsSearching(!isSearching)
    }

    const handleBlur = () => {
        setIsSearching(false)
    }

    return (
        <div className="search-container">
            <AutoComplete
                value={searchValue}
                onChange={handleSearch}
                onSelect={handleSelect}
                autoFocus
                options={options.map(option => ({
                    value: option.value,
                    label: <span>{option.label}</span>, // 展示菜单项
                }))}
            >
                <Input
                    ref={inputRef}
                    placeholder="请输入菜单名称"
                    className={`search-input ${isSearching ? 'active' : ''}`}
                    style={{ width: isSearching ? 200 : 0 }}
                    onBlur={handleBlur}
                    transition="width 0.3s ease-in-out"
                />
            </AutoComplete>
            <IconBox className={`search-button ${isSearching ? 'hidden' : ''}`} onClick={handleClick}>
                <SearchOutlined style={{fontSize: '20px'}} />
            </IconBox>
        </div>
    )
}

export default SearchMenu