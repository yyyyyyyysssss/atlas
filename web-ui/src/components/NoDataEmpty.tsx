import { Empty } from 'antd';

const NoDataEmpty: React.FC = () => {
    return (
        <Empty image={Empty.PRESENTED_IMAGE_DEFAULT} description="暂无数据" />
    )
}

export default NoDataEmpty