import './index.css';
import { List, theme, App, Card, Avatar, Typography, Tooltip, Flex, Button } from "antd";
import { useNavigate } from "react-router-dom";
import { useInfiniteScroll, useRequest } from "ahooks";
import useFullParams from '../../../../../../hooks/useFullParams'
import { AppstoreOutlined, DeleteOutlined, EditOutlined } from "@ant-design/icons";
import { deleteSaml2Client, getSaml2ClientPage } from '../../../../../../services/ApplicationService';
import { useRef } from 'react';
import { useDomain } from '../../../../../../router/DomainProvider';
import Loading from '../../../../../../components/loading';
import ClientListItem from '../../../components/ClientListItem';


const { Text } = Typography;
const { useToken } = theme;
const PAGE_SIZE = 10;

const Saml2ClientApplicationList = () => {

    const { token } = useToken();
    const navigate = useNavigate();
    const scrollRef = useRef(null);
    const { message, modal } = App.useApp();

    const { applicationId } = useFullParams()

    const { runAsync: deleteSaml2ClientAsync, loading: deleteSaml2ClientLoading } = useRequest(deleteSaml2Client, {
        manual: true
    });

    const { domainId } = useDomain()

    const {
        data,
        loading,
        loadingMore,
        noMore,
        mutate
    } = useInfiniteScroll(
        async (d) => {
            const pageNum = d ? d.nextPage : 1;
            const res = await getSaml2ClientPage(domainId, applicationId, pageNum, PAGE_SIZE);
            const hasMore = pageNum * PAGE_SIZE < res.total;

            return {
                list: res.list,
                total: res.total,
                nextPage: hasMore ? pageNum + 1 : pageNum,
                hasMore: hasMore
            };
        },
        {
            target: scrollRef,
            isNoMore: (d) => !d?.hasMore,
        }
    );

    const clientList = data?.list || [];


    const handleDeleteClient = (id, e) => {
        // 阻止点击删除按钮时触发 List.Item 的 onClick 路由跳转
        e?.stopPropagation();

        modal.confirm({
            title: '确定要删除该 Saml2 应用吗？',
            content: '删除后，该客户端已颁发的 Token 也会被强制注销，此操作不可逆！',
            okText: '确定删除',
            okType: 'danger',
            loading: deleteSaml2ClientLoading,
            cancelText: '取消',
            onOk: async () => {
                await deleteSaml2ClientAsync(domainId, applicationId, id);
                message.success('应用删除成功');
                if (data) {
                    mutate({
                        ...data,
                        total: data.total - 1,
                        list: data.list.filter(item => item.id !== id)
                    });
                }
            }
        });
    };

    return (
        <div
            ref={scrollRef}
            style={{
                height: 550,
                overflow: "auto",
                paddingRight: 8
            }}
        >
            {loading && clientList.length === 0 ? (
                <div style={{ padding: 20 }}>
                    <Loading spinning />
                </div>
            ) : (
                <>
                    <List
                        dataSource={clientList}
                        renderItem={(client) => (
                            <List.Item
                                key={client.id}
                                onClick={() => navigate(`/project/${domainId}/application/${applicationId}/client/saml2/${client.id}`)}
                                style={{ cursor: "pointer", padding: "12px 0" }}
                            >
                                <div style={{ width: "100%" }}>
                                    <ClientListItem
                                        client={client}
                                        onDelete={(id, e) => handleDeleteClient(id, e)}
                                        clientIdLabel='SP Entity ID'
                                    />
                                </div>
                            </List.Item>
                        )}
                    />

                    {loadingMore && (
                        <div style={{ textAlign: "center", padding: 12 }}>
                            <Loading spinning size="small" />
                        </div>
                    )}

                    {noMore && clientList.length > PAGE_SIZE && (
                        <div style={{ textAlign: "center", padding: 12, color: token.colorTextTertiary }}>
                            没有更多数据了
                        </div>
                    )}
                </>
            )}
        </div>
    )
}

export default Saml2ClientApplicationList