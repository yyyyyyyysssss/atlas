import React, { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Flex, Card, Button, message, Typography, Avatar, Alert, Divider, Popover, theme } from "antd"
import logo from '/favicon.ico'
import './index.css'
import useFullParams from '../../../hooks/useFullParams'
import httpWrapper from '../../../services/AxiosWrapper'
import { urlParamParse } from '../../../utils/UrlUtil'
import { useSelector } from 'react-redux'
import { useRequest } from 'ahooks'
import Loading from '../../../components/loading'
import ScopeConfirm from './components/ScopeConfirm'
import { useAuth } from '../../../router/AuthProvider'

const Consent = () => {
    const { token } = theme.useToken()

    const { accessToken } = useAuth()
    //路由参数
    const params = useFullParams()

    const { username, fullName, avatar } = useSelector(state => state.user.userInfo)

    const [step, setStep] = useState(1); // 1: 身份确认, 2: 授权确认


    const { loading, run: runConsent } = useRequest(
        async () => {
            const pm = params;
            let requestUrl;
            let requestData = new FormData();

            if (pm.type === 'code') {
                requestUrl = httpWrapper.getUri() + '/api/auth/oauth2/authorize';
            } else if (pm.type === 'device') {
                requestUrl = httpWrapper.getUri() + '/api/auth/oauth2/device_verification';
                requestData.append('user_code', pm.user_code);
            } else {
                throw new Error("未知的类型");
            }

            const scopes = pm.scope.split(' ');
            requestData.append('client_id', pm.client_id);
            requestData.append('state', pm.state);
            scopes.forEach(s => requestData.append('scope', s));

            // 返回 fetch 结果给 useRequest 处理
            return fetch(requestUrl, {
                method: 'post',
                redirect: "follow", // 改为 manual 以处理重定向或特殊逻辑
                body: requestData,
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                }
            });
        },
        {
            manual: true, // 设置为手动触发
            onSuccess: async (res) => {
                console.log('res', res)
                if (res.type === 'opaqueredirect' || res.status === 302 || res.redirected) {
                    window.location.href = res.url;
                } else if (res.status === 200) {
                    // 如果后端返回 200，可能需要处理 JSON 结果
                    const data = await res.json();
                    if (data.redirectUrl) {
                        window.location.href = data.redirectUrl;
                    }
                }
            },
            onError: (err) => {
                console.error(err + " url: " + params.client_id);
                message.error(err.message || "请求失败");
            },
            // 防止用户快速连击按钮导致发送多次授权请求
            throttleWait: 1000,
        }
    )

    const handleConfirmIdentity = () => {
        setStep(2);
    }

    const handleCancel = () => {
        window.history.back();
    }

    return (

        <Flex style={{ minHeight: '100vh', backgroundColor: token.colorBgContainer }}>
            <Flex gap='middle' justify='center' align='center' vertical style={{ width: '100%' }}>
                <Loading spinning={loading}>
                    {step === 1 ? (
                        <Card
                            title={
                                <Flex gap={10} align='center'>
                                    <Avatar src={logo} />
                                    <Typography.Title level={4} style={{ margin: 0 }}>使用Atlas账号登录</Typography.Title>
                                </Flex>
                            }
                            style={{ width: '800px', height: '344px', overflow: 'hidden', borderRadius: "20px", boxShadow: token.boxShadowTertiary }}
                        >
                            <Flex gap='middle' style={{ height: '100%', width: '100%' }}>
                                {/* 左侧：应用身份区 */}
                                <Flex gap={10} style={{ width: '50%', height: '100%', paddingLeft: '10px' }} vertical>
                                    <Avatar src={params.logo_uri} size={48} />
                                    <Typography.Text style={{ fontSize: 30 }}>请选择账号</Typography.Text>
                                    {params.type === 'device' && params.user_code && (
                                        <Alert
                                            message={
                                                <Flex vertical gap={4}>
                                                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                                        请确认您设备上显示的码是否为：
                                                    </Typography.Text>
                                                    <Typography.Text strong style={{ fontSize: 18, letterSpacing: '2px', color: token.colorPrimary }} copyable>
                                                        {params.user_code}
                                                    </Typography.Text>
                                                </Flex>
                                            }
                                            type="info"
                                            showIcon
                                            style={{
                                                marginBottom: '10px',
                                                width: 'fit-content',
                                                borderRadius: '8px',
                                                backgroundColor: token.colorFillAlter,
                                                borderColor: token.colorBorderSecondary
                                            }}
                                        />
                                    )}
                                    <Typography.Paragraph
                                        style={{
                                            fontSize: 18,
                                            lineHeight: '1.6',
                                            margin: 0,
                                            textAlign: 'justify' // 让文字两端对齐，看起来更专业
                                        }}
                                    >
                                        继续前往
                                        <Popover
                                            trigger="click"
                                            placement="bottomLeft"
                                            styles={{
                                                body: {
                                                    borderRadius: 12,
                                                    padding: '16px'
                                                }
                                            }}
                                            content={
                                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                                    <Flex vertical>
                                                        <Typography.Text style={{ fontSize: 12 }} type='secondary'>开发者邮件</Typography.Text>
                                                        <Typography.Text copyable>{params.developer_email}</Typography.Text>
                                                    </Flex>
                                                    <Flex vertical>
                                                        <Typography.Text style={{ fontSize: 12 }} type='secondary'>官方主页</Typography.Text>
                                                        <Typography.Link href={params.app_domain} target="_blank">
                                                            {params.app_domain}
                                                        </Typography.Link>
                                                    </Flex>
                                                </div>
                                            }
                                        >
                                            <Typography.Link style={{ fontSize: 18, marginLeft: 8, textDecoration: 'underline' }}>
                                                {params.client_name}
                                            </Typography.Link>
                                        </Popover>
                                        {/* <Typography.Link style={{ fontSize: 18 }}>{params.client_name}</Typography.Link> */}
                                    </Typography.Paragraph>
                                </Flex>

                                {/* 右侧：用户信息及政策区 */}
                                <Flex style={{ width: '50%', height: '100%', marginTop: '40px' }} vertical>
                                    <Flex justify='center' vertical>
                                        <Button
                                            type="text"
                                            onClick={handleConfirmIdentity}
                                            style={{
                                                height: 'auto',     // 允许按钮由内部 Avatar 撑开高度
                                                width: '100%',
                                                textAlign: 'left',
                                                display: 'flex',
                                                justifyContent: 'flex-start',
                                            }}
                                        >
                                            <Flex align='center' gap={10} >
                                                <Avatar src={avatar} />
                                                <Flex vertical>
                                                    <Typography.Title level={5} style={{ margin: 0 }}>{fullName}</Typography.Title>
                                                    <Typography.Text type='secondary'>{username}</Typography.Text>
                                                </Flex>
                                            </Flex>
                                        </Button>
                                        <Divider />
                                    </Flex>
                                    <Typography.Paragraph
                                        type="secondary"
                                        style={{
                                            fontSize: 14,
                                            lineHeight: '1.6',
                                            margin: 0,
                                            textAlign: 'justify' // 让文字两端对齐，看起来更专业
                                        }}
                                    >
                                        建议您在使用“<Typography.Text strong size="small">{params.client_name}</Typography.Text>”之前，先阅读此应用的
                                        <Button type="link" target="_blank" rel="noopener noreferrer" size="small" href={params.privacy_uri} style={{ fontSize: 14 }}>
                                            《隐私权政策》
                                        </Button>
                                        和
                                        <Button type="link" target="_blank" rel="noopener noreferrer" size="small" href={params.terms_uri} style={{ fontSize: 14 }}>
                                            《服务条款》
                                        </Button>
                                        。
                                    </Typography.Paragraph>
                                </Flex>
                            </Flex>
                        </Card>
                    ) : (
                        <ScopeConfirm
                            params={params}
                            onConfirm={runConsent}
                            onCancel={() => setStep(1)}
                            loading={loading}
                        />
                    )}
                </Loading>
            </Flex>
        </Flex>
    );
}


export default Consent;