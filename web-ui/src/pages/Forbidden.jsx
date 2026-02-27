import { Button, Result } from "antd"
import { useNavigate } from 'react-router-dom'



const Forbidden = () => {

    const navigate = useNavigate()

    const goHome = () => {
        navigate('/home')
    }

    return (
        <Result
            status="403"
            title="403"
            subTitle="您没有权限访问此页面"
            extra={<Button onClick={goHome} type="primary">返回主页</Button>}
        />
    )
}

export default Forbidden