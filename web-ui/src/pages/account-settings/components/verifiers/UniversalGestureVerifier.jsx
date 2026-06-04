import React, { useRef, useState, useEffect, useCallback } from 'react';
import { Typography, theme, Flex, App } from 'antd';

const { Text } = Typography;

// 3x3 矩阵的 9 个标准圆点相对比例坐标 (基于 300x300 画布)
const GRID_POINTS = [
    { id: '1', x: 50,  y: 50  }, { id: '2', x: 150, y: 50  }, { id: '3', x: 250, y: 50  },
    { id: '4', x: 50,  y: 150 }, { id: '5', x: 150, y: 150 }, { id: '6', x: 250, y: 150 },
    { id: '7', x: 50,  y: 250 }, { id: '8', x: 150, y: 250 }, { id: '9', x: 250, y: 250 }
];

const CANVAS_SIZE = 300;
const POINT_RADIUS = 12; // 默认静态圆点半径
const HIT_RADIUS = 26;   // 判定划过的触碰敏感半径

/**
 * 全站通用的图案手势连线快速鉴权组件（原生 status 支持 + 物理穿透补点版）
 */
const UniversalGestureVerifier = ({
    verifierRef,
    onVerifyAction,
    onSuccess,
    label = "手势连线验证",
    status = 'default' // 🚀 新增原生支持：'default' | 'error'
}) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();

    const canvasRef = useRef(null);
    const [isDrawing, setIsDrawing] = useState(false);
    const [selectedPoints, setSelectedPoints] = useState([]); 
    const [currentMousePos, setCurrentMousePos] = useState(null); 
    const [verifyLoading, setVerifyLoading] = useState(false);
    const [localErrorState, setLocalErrorState] = useState(false); 

    // 🚀 核心状态桥接：无论是父级传入 'error' 还是本地抛出错误，都判定为错误状态
    const isErrorState = status === 'error' || localErrorState;
    const isGlobalLoading = verifyLoading;

    // 清空与重置画布状态
    const handleReset = useCallback(() => {
        setSelectedPoints([]);
        setCurrentMousePos(null);
        setIsDrawing(false);
        setLocalErrorState(false);
    }, []);

    // 核心重绘引擎：监听状态变更自动驱动 Canvas 2D 渲染流
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        
        const dpr = window.devicePixelRatio || 1;
        if (canvas.width !== CANVAS_SIZE * dpr || canvas.height !== CANVAS_SIZE * dpr) {
            canvas.width = CANVAS_SIZE * dpr;
            canvas.height = CANVAS_SIZE * dpr;
        }
        
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        // 🚀 核心优化：动态计算语义着色（如果是 error 状态，全部转成 AntD 官方红）
        const activeColor = isErrorState ? token.colorError : token.colorPrimary;
        const activeBgColor = isErrorState ? token.colorErrorBg : token.colorPrimaryBg;

        // 1. 绘制已生成的连接线段
        if (selectedPoints.length > 0) {
            ctx.beginPath();
            ctx.strokeStyle = activeColor;
            ctx.lineWidth = 4;
            ctx.lineJoin = 'round';
            ctx.lineCap = 'round';

            selectedPoints.forEach((pointId, index) => {
                const pt = GRID_POINTS.find(p => p.id === pointId);
                if (index === 0) ctx.moveTo(pt.x, pt.y);
                else ctx.lineTo(pt.x, pt.y);
            });

            if (isDrawing && currentMousePos && !isErrorState) {
                ctx.lineTo(currentMousePos.x, currentMousePos.y);
            }
            ctx.stroke();
        }

        // 2. 循环绘制 9 个基础控制原点
        GRID_POINTS.forEach((pt) => {
            const isSelected = selectedPoints.includes(pt.id);

            // 绘制矩阵外圈
            ctx.beginPath();
            ctx.arc(pt.x, pt.y, POINT_RADIUS + 4, 0, Math.PI * 2);
            ctx.fillStyle = isSelected ? activeBgColor : (token.mode === 'dark' ? token.colorFillSecondary : token.colorFillAlter);
            ctx.fill();

            // 绘制核心内点
            ctx.beginPath();
            ctx.arc(pt.x, pt.y, POINT_RADIUS / 2, 0, Math.PI * 2);
            ctx.fillStyle = isSelected ? activeColor : (token.mode === 'dark' ? token.colorTextSecondary : token.colorTextDescription);
            ctx.fill();
        });
    }, [selectedPoints, isDrawing, currentMousePos, isErrorState, token]);

    const getCanvasRelativePosition = (e) => {
        const canvas = canvasRef.current;
        if (!canvas) return { x: 0, y: 0 };
        const rect = canvas.getBoundingClientRect();
        
        const clientX = e.touches && e.touches.length > 0 ? e.touches[0].clientX : e.clientX;
        const clientY = e.touches && e.touches.length > 0 ? e.touches[0].clientY : e.clientY;
        
        return {
            x: clientX - rect.left,
            y: clientY - rect.top
        };
    };

    // 基础碰撞检测
    const detectCollisionPoint = (pos, currentSelected) => {
        for (let pt of GRID_POINTS) {
            if (currentSelected.includes(pt.id)) continue;
            const distance = Math.sqrt(Math.pow(pos.x - pt.x, 2) + Math.pow(pos.y - pt.y, 2));
            if (distance <= HIT_RADIUS) {
                return pt.id;
            }
        }
        return null;
    };

    const doServerAuthenticate = async (gestureSequence) => {
        if (gestureSequence.length < 4) {
            setLocalErrorState(true);
            message.warning('手势连线过短，请至少连接 4 个点');
            setTimeout(() => handleReset(), 400);
            return;
        }

        try {
            setVerifyLoading(true);
            const result = await onVerifyAction(gestureSequence);
            if (!result || result.verified === false) {
                throw new Error('手势密码验证失败，请重试');
            }
            return result;
        } catch (error) {
            setLocalErrorState(true);
            setTimeout(() => handleReset(), 1000);
            throw error;
        } finally {
            setVerifyLoading(false);
        }
    };

    const handleInteractionStart = (e) => {
        if (isGlobalLoading || isErrorState) return;
        e.preventDefault();
        setIsDrawing(true);
        
        const pos = getCanvasRelativePosition(e);
        const hitId = detectCollisionPoint(pos, []);
        if (hitId) {
            setSelectedPoints([hitId]);
        }
    };

    const handleInteractionMove = (e) => {
        if (!isDrawing || isGlobalLoading || isErrorState) return;
        e.preventDefault();
        
        const pos = getCanvasRelativePosition(e);
        setCurrentMousePos(pos);

        const hitId = detectCollisionPoint(pos, selectedPoints);
        if (hitId) {
            setSelectedPoints(prev => {
                if (prev.length === 0) return [hitId];
                
                const lastId = prev[prev.length - 1];
                const lastPt = GRID_POINTS.find(p => p.id === lastId);
                const currPt = GRID_POINTS.find(p => p.id === hitId);

                const midX = (lastPt.x + currPt.x) / 2;
                const midY = (lastPt.y + currPt.y) / 2;

                const middlePoint = GRID_POINTS.find(p => Math.abs(p.x - midX) < 1 && Math.abs(p.y - midY) < 1);

                if (middlePoint && !prev.includes(middlePoint.id)) {
                    return [...prev, middlePoint.id, hitId];
                }

                return [...prev, hitId];
            });
        }
    };

    const handleInteractionEnd = useCallback(async () => {
        if (!isDrawing || isGlobalLoading || isErrorState) return;
        setIsDrawing(false);
        setCurrentMousePos(null);

        if (selectedPoints.length > 0) {
            const gestureSequence = selectedPoints.join('');
            const result = await doServerAuthenticate(gestureSequence);
            if (result && onSuccess) {
                onSuccess(result);
            }
        }
    }, [isDrawing, isGlobalLoading, isErrorState, selectedPoints]);

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleReset();
            message.info('手势画布已重置，请重新绘制');
        }
    };

    if (verifierRef) {
        verifierRef.current = {
            getValue: () => selectedPoints.join(''),
            validate: async () => {
                if (selectedPoints.length < 4) {
                    setLocalErrorState(true);
                    throw new Error('手势连线过短，无法提交验证。');
                }
                return true;
            },
            onVerify: async () => {
                const seq = selectedPoints.join('');
                return await doServerAuthenticate(seq);
            },
            reset: () => handleReset()
        };
    }

    const getTipText = () => {
        if (verifyLoading) return '正在验证手势序列...';
        if (isErrorState) return '手势错误，请重新绘制';
        if (isDrawing) return '请继续滑行绘制图案...';
        return '请滑动连线完成图形图案解锁';
    };

    return (
        <Flex vertical gap={8} style={{ width: '100%' }}>
            {/* 仅在传入 label 时渲染，避免影响外层定制排版 */}
            {label && <Text style={{ fontWeight: 500, fontSize: 13 }}>{label}</Text>}
            <Flex
                align="center"
                justify="center"
                vertical
                tabIndex={isGlobalLoading ? -1 : 0}
                onKeyDown={handleKeyDown}
                
                onMouseUp={handleInteractionEnd}
                onTouchEnd={handleInteractionEnd}
                onMouseLeave={handleInteractionEnd} 
                
                style={{
                    padding: '24px',
                    borderRadius: token.borderRadiusLG,
                    background: isGlobalLoading ? token.colorFillTertiary : token.colorFillAlter,
                    transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
                    outline: 'none',
                    border: `1px solid ${isErrorState ? token.colorErrorBorder : 'transparent'}`,
                    // 🚀 如果父级判定为 error，同样拒绝鼠标交互乱划
                    pointerEvents: isErrorState ? 'none' : 'auto'
                }}
                className={`gesture-verifier-card ${isErrorState ? 'shake-animation' : ''}`}
            >
                <div 
                    style={{ 
                        position: 'relative', 
                        width: `${CANVAS_SIZE}px`, 
                        height: `${CANVAS_SIZE}px`,
                        minWidth: `${CANVAS_SIZE}px`,
                        minHeight: `${CANVAS_SIZE}px`,
                        marginBottom: 16,
                        overflow: 'visible' 
                    }}
                >
                    <canvas
                        ref={canvasRef}
                        onMouseDown={handleInteractionStart}
                        onMouseMove={handleInteractionMove}
                        onTouchStart={handleInteractionStart}
                        onTouchMove={handleInteractionMove}
                        style={{ 
                            cursor: isGlobalLoading ? 'not-allowed' : 'crosshair',
                            touchAction: 'none',
                            borderRadius: token.borderRadiusLG,
                            width: `${CANVAS_SIZE}px`,
                            height: `${CANVAS_SIZE}px`,
                            display: 'block'
                        }}
                    />
                </div>

                <Text strong style={{ color: isErrorState ? token.colorError : (isDrawing ? token.colorPrimary : token.colorText) }}>
                    {getTipText()}
                </Text>
                <Text type="secondary" style={{ fontSize: 12, marginTop: 4 }}>
                    键盘无障碍支持：按下 Space/Enter 键可随时重置画布
                </Text>
            </Flex>

            <style>{`
                @keyframes shake {
                    0%, 100% { transform: translateX(0); }
                    20%, 60% { transform: translateX(-6px); }
                    40%, 80% { transform: translateX(6px); }
                }
                
                .shake-animation {
                    animation: shake 0.4s ease-in-out;
                }

                .gesture-verifier-card {
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
                }

                .gesture-verifier-card:hover {
                    background: ${token.colorFillSecondary} !important;
                    border-color: ${isErrorState ? token.colorErrorBorder : token.colorBorderSecondary} !important;
                }
            `}</style>
        </Flex>
    );
};

export default UniversalGestureVerifier;