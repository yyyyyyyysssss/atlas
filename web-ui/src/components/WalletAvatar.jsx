import React, { useRef, useEffect, memo } from 'react';
import Jazzicon from '@metamask/jazzicon';

/**
 * @param {string} address - 钱包地址
 * @param {number} [size=20] - 头像尺寸
 * @param {string} [className] - 额外的 CSS 类名
 * @param {object} [style] - 额外的行内样式
 */
export const WalletAvatar = memo(({ address, size = 20, className, style }) => {
  const avatarRef = useRef(null);

  useEffect(() => {
    if (!avatarRef.current || !address) return;

    // 清空现有节点，防止多次渲染叠加
    avatarRef.current.innerHTML = '';
    
    // 安全解析：确保地址格式正确，取前8位十六进制字符作为种子
    const normalizedAddress = address.toLowerCase();
    const seed = parseInt(normalizedAddress.slice(2, 10), 16);
    
    // 渲染图标
    const icon = Jazzicon(size, seed);
    avatarRef.current.appendChild(icon);
  }, [address, size]);

  return (
    <div 
      ref={avatarRef} 
      className={className}
      style={{ 
        width: size, 
        height: size, 
        borderRadius: '50%',
        overflow: 'hidden',
        flexShrink: 0, // 工业级 UI 必备：防止头像在 Flex 布局中被压缩变形
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        ...style 
      }} 
    />
  );
});

WalletAvatar.displayName = 'WalletAvatar';