import { Button, Result } from "antd"
import { useNavigate } from 'react-router-dom'



const NotFound = () => {

    const navigate = useNavigate()

    const goHome = () => {
        navigate('/home')
    }

    return (
        <Result
            status="404"
            title="404"
            subTitle="您访问的页面不存在"
            extra={<Button onClick={goHome} type="primary">返回主页</Button>}
        />
    )
}

export default NotFound