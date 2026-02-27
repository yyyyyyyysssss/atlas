import React, { FC } from 'react';
import './index.css';


export interface IconBoxProps extends React.HTMLProps<HTMLDivElement>{
    icon?: React.ReactNode;
    children?: React.ReactNode;
    padding?: number;                        // 控制点击区域大小
    className?: string;
    onClick?: () => void;
}

const IconBox: FC<IconBoxProps> = ({
    icon,
    children,
    padding = 6,
    className = '',
    onClick,
    ...rest
}) => {

    const content = children ?? icon;

    return (
        <div
            className={`icon-button ${className}`}
            style={{
                padding,
            }}
            onClick={onClick}
            {...rest}
        >
            {content}
        </div>
    )
}

export default IconBox