import { Button, Flex, Result } from "antd"
import { useNavigate } from 'react-router-dom';
import useFullParams from "../hooks/useFullParams";
import CopyToClipboard from "../components/CopyToClipboard";


const Success = () => {

    const { title, code, listRouterPath, againRouterPath } = useFullParams()

    const navigate = useNavigate()

    const goList = () => {
        navigate(listRouterPath)
    }

    const again = () => {
        navigate(againRouterPath)
    }

    return (
        <Result
            status="success"
            title={title}
            subTitle={<Flex justify="center" align="center">编号：<CopyToClipboard text={code} /></Flex>}
            extra={[
                <Button type="primary" onClick={goList} key='goList'>
                    返回列表
                </Button>,
                <Button onClick={again} key='again'>再次创建</Button>,
            ]}
        />
    )
}

export default Success