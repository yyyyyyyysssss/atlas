import useFullParams from '../../../hooks/useFullParams'
import './index.css'
import { Typography } from "antd"



const ProjectApplication = () => {

    const { domainId } = useFullParams()

    const projectId = domainId

    return (
        <Typography.Title>ProjectApplication: {projectId}</Typography.Title>
    )
}

export default ProjectApplication