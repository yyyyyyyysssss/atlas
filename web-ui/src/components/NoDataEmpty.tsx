import { Empty } from 'antd';

interface NoDataEmptyProps {
    description?: string;
    children?: React.ReactNode;
}

const NoDataEmpty: React.FC<NoDataEmptyProps> = ({
    description = "暂无数据",
    children
}) => {
    return (
        <Empty
            image={Empty.PRESENTED_IMAGE_DEFAULT}
            description={description}
        >
            {children}
        </Empty>
    );
}

export default NoDataEmpty;