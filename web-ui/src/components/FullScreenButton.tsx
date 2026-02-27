import { Tooltip } from 'antd';
import React, { useEffect, useState } from 'react';
import { FullscreenOutlined } from '@ant-design/icons';
import IconBox from './icon-box';

interface FullScreenButtonProps {
    targetRef: React.RefObject<HTMLElement>; // 外部传入的 ref
}

const FullScreenButton: React.FC<FullScreenButtonProps> = ({ targetRef }) => {

    const [isFullScreen, setIsFullScreen] = useState(false)

    useEffect(() => {
        const handleFullScreenChange = () => {
            const isFullScreen = document.fullscreenElement != null; // 判断是否为全屏
            setIsFullScreen(isFullScreen)
        }

        document.addEventListener('fullscreenchange', handleFullScreenChange)
        document.addEventListener('webkitfullscreenchange', handleFullScreenChange) // Safari
        document.addEventListener('mozfullscreenchange', handleFullScreenChange) // Firefox
        document.addEventListener('MSFullscreenChange', handleFullScreenChange) // IE/Edge

        // 清理事件监听器
        return () => {
            document.removeEventListener('fullscreenchange', handleFullScreenChange)
            document.removeEventListener('webkitfullscreenchange', handleFullScreenChange)
            document.removeEventListener('mozfullscreenchange', handleFullScreenChange)
            document.removeEventListener('MSFullscreenChange', handleFullScreenChange)
        }
    }, [])

    const enterFullScreen = () => {
        const element = targetRef.current as HTMLElement
        if (element) {
            if (element.requestFullscreen) {
                element.requestFullscreen()
            } else if ((element as any).mozRequestFullScreen) { // Firefox
                (element as any).mozRequestFullScreen()
            } else if ((element as any).webkitRequestFullscreen) { // Chrome, Safari, Opera
                (element as any).webkitRequestFullscreen()
            } else if ((element as any).msRequestFullscreen) { // IE/Edge
                (element as any).msRequestFullscreen()
            }
            element.style.overflow = 'auto'
            setIsFullScreen(true)
        }

    }

    const exitFullScreen = () => {
        if (document.exitFullscreen) {
            document.exitFullscreen()
        } else if ((document as any).mozCancelFullScreen) { // Firefox
            (document as any).mozCancelFullScreen()
        } else if ((document as any).webkitExitFullscreen) { // Chrome, Safari, Opera
            (document as any).webkitExitFullscreen()
        } else if ((document as any).msExitFullscreen) { // IE/Edge
            (document as any).msExitFullscreen()
        }
        setIsFullScreen(false)
    }

    const toggleFullScreen = () => {
        if (isFullScreen) {
            exitFullScreen()
        } else {
            enterFullScreen()
        }
    }

    return (
        <>
            <IconBox
                onClick={toggleFullScreen}
            >
                {!isFullScreen && (
                    <Tooltip title='进入全屏'>
                        <FullscreenOutlined style={{ fontSize: '20px' }} />
                    </Tooltip>
                )}
            </IconBox>
        </>
    )
}

export default FullScreenButton