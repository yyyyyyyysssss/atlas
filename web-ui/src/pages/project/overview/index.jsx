import useFullParams from '../../../hooks/useFullParams'
import './index.css'
import { Typography } from "antd"



const ProjectOverview = () => {

    const { domainId  } = useFullParams()

    const projectId = domainId

    return (
        <Typography.Title>ProjectOverview: {projectId}</Typography.Title>
    )
}

export default ProjectOverview