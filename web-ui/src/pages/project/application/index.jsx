import useFullParams from '../../../hooks/useFullParams'
import './index.css'
import { Typography } from "antd"



const ProjectApplication = () => {
 
    const { projectId } = useFullParams()
    
    return (
        <Typography.Title>ProjectApplication: {projectId}</Typography.Title>
    )
}

export default ProjectApplication