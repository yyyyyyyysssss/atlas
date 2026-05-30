import { Typography } from 'antd'
import useFullParams from '../../../hooks/useFullParams'
import './index.css'



const LoginMfa = () => {

    const { ticket, mfaType } = useFullParams()

    return (
        <Typography.Title>Login Mfa</Typography.Title>
    )
}

export default LoginMfa