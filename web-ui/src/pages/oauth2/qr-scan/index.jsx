import { Flex, theme, message, Result, Button } from 'antd';
import { useSearchParams, useNavigate } from 'react-router-dom';
import ScopeConfirm from '../components/ScopeConfirm';
import './index.css'
import useFullParams from '../../../hooks/useFullParams';
import { useRequest } from 'ahooks';
import { qrConfirm, qrScan } from '../../../services/Oauth2Service';
import { useEffect, useState } from 'react';

const QrScan = () => {
    const { token } = theme.useToken()
    const params = useFullParams()
    const navigate = useNavigate()
    const [isConfirmed, setIsConfirmed] = useState(false)

    const { runAsync: qrScanAsync, loading: qrScanLoading } = useRequest(qrScan, {
        manual: true,
        onError: () => {
            message.error('二维码可能已过期或状态异常，请重试');
        }
    })

    const { runAsync: qrConfirmAsync, loading: qrConfirmLoading } = useRequest(qrConfirm, {
        manual: true,
        onSuccess: () => {
            setIsConfirmed(true)
        },
        onError: (err) => {
            message.error(err.message || '授权确认失败，请重试');
        }
    })

    useEffect(() => {
        if (params.scene_id) {
            qrScanAsync(params.scene_id)
        }
    },[params.scene_id])

    const handleConfirm = async () => {
        if (params.scene_id) {
            await qrConfirmAsync(params.scene_id)
        }
    }

    const handleCancel = () => {
        message.info('您已取消授权操作')
        // 根据业务需求，取消时可以退回首页
        navigate('/')
    }

    return (
        <Flex
            style={{
                minHeight: '100vh',
                backgroundColor: token.colorBgContainer
            }}
            justify="center"
            align="center"
        >
            {isConfirmed ? (
                <div style={{
                    width: '500px',
                    padding: '40px',
                    borderRadius: "20px",
                    background: token.colorBgContainer
                }}>
                    <Result
                        status="success"
                        title="授权成功！"
                        subTitle="您已成功授权网页端登录。请回到电脑屏幕前继续操作。"
                        extra={[
                            <Button type="primary" key="home" onClick={() => navigate('/')}>
                                返回主页
                            </Button>,
                        ]}
                    />
                </div>
            ) : (
                <ScopeConfirm
                    params={params}
                    onConfirm={handleConfirm}
                    onCancel={handleCancel}
                    loading={qrConfirmLoading} 
                />
            )}
        </Flex>
    )
}

export default QrScan