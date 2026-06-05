import { Card, Flex } from "antd"
import { Monitor } from "lucide-react";
import UniversalGestureVerifier from "../../account-settings/components/verifiers/UniversalGestureVerifier";
import { useRef } from "react";
import { gestureLogin } from "../../../services/LoginService";
import { useRequest } from "ahooks";


const GestureLoginCard = ({ loginPanel, setLoginPanel, loginSuccessHandler }) => {

    const verifierRef = useRef(null)

    const { runAsync: gestureLoginAsync, loading: gestureLoginLoading } = useRequest(gestureLogin, {
        manual: true
    })

    const login = () => {

    }

    return (
        <Card
            style={{
                width: '100%',
                height: '100%',
                borderRadius: '24px',
                boxShadow: '0 25px 50px -12px rgba(0,0,0,0.05)',
                border: 'none',
                background: '#ffffff',
                padding: '16px',
            }}
        >
            <div
                style={{
                    position: 'absolute',
                    top: 16,
                    right: 16,
                    cursor: 'pointer',
                    zIndex: 10,
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                }}
                onClick={() => setLoginPanel('main')}
                title="密码登录"
                onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'scale(1.1)';
                    e.currentTarget.style.opacity = '0.8';
                }}
                onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'scale(1)';
                    e.currentTarget.style.opacity = '1';
                }}
            >
                <Monitor
                    size={32}
                    color="#9ca3af"
                    strokeWidth={1.5}
                />
            </div>

            <Flex vertical align="center" justify="center" style={{ height: '100%', paddingTop: 20 }}>
                <UniversalGestureVerifier
                    label=""
                    verifierRef={verifierRef}
                    onVerifyAction={login}
                    onSuccess={loginSuccessHandler}
                />
            </Flex>
        </Card>
    )
}

export default GestureLoginCard