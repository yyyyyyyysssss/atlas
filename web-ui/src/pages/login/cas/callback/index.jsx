import { Typography } from 'antd'
import useFullParams from '../../../../hooks/useFullParams'
import './index.css'


const CasCallback = () => {

    const { clientName, ticket } = useFullParams()

    return (
        <Typography.Title>{ticket}</Typography.Title>
    )
}

export default CasCallback