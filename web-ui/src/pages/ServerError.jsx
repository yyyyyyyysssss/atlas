import { Button, Result } from "antd"
import { useNavigate, useLocation } from 'react-router-dom'



const ServerError = () => {

    const navigate = useNavigate()
    const location = useLocation()

    const { title = '500', subTitle = '抱歉，页面出了点问题，请稍后再试' } = location.state || {}

    const goHome = () => {
        navigate('/home')
    }

    return (
        <Result
            status="500"
            title={title}
            subTitle={subTitle}
            extra={<Button onClick={goHome} type="primary">返回主页</Button>}
        />
    )
}

export default ServerError