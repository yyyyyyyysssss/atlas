import useFullParams from '../../../hooks/useFullParams'
import './index.css'
import { Typography } from "antd"



const ProjectOverview = () => {

    const { projectId } = useFullParams()

    return (
        <Typography.Title>ProjectOverview: {projectId}</Typography.Title>
    )
}

export default ProjectOverview