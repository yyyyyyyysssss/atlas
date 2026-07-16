import React, { useRef } from "react";
import './index.css';
import { List, theme, App } from "antd";
import OAuth2ClientApplicationItem from "../../components/OAuth2ClientApplicationItem";
import { useNavigate } from "react-router-dom";
import { useInfiniteScroll, useRequest } from "ahooks";
import { deleteApplication, getApplicationPage } from "../../../../services/DeveloperSettingsService";
import Loading from "../../../../components/loading";

const { useToken } = theme;
const PAGE_SIZE = 10;

const OAuth2ClientApplicationList = () => {
    const { token } = useToken();
    const navigate = useNavigate();
    const scrollRef = useRef(null);
    const { message, modal } = App.useApp();

    const { runAsync: deleteApplicationAsync, loading: deleteApplicationLoading } = useRequest(deleteApplication, {
        manual: true
    });

    const {
        data,
        loading,
        loadingMore,
        noMore,
        mutate
    } = useInfiniteScroll(
        async (d) => {
            const pageNum = d ? d.nextPage : 1;
            const res = await getApplicationPage(pageNum, PAGE_SIZE);
            const hasMore = pageNum * PAGE_SIZE < res.total;

            return {
                list: d?.list ? [...d.list, ...res.list] : res.list,
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

    const appList = data?.list || [];

    const handleDeleteApplication = (id, e) => {
        // 阻止点击删除按钮时触发 List.Item 的 onClick 路由跳转
        e?.stopPropagation(); 
        
        modal.confirm({
            title: '确定要删除该 OAuth2 应用吗？',
            content: '删除后，该应用分配的 Client Secret 将全部失效，已颁发的 Token 也会被强制注销，此操作不可逆！',
            okText: '确定删除',
            okType: 'danger',
            loading: deleteApplicationLoading,
            cancelText: '取消',
            onOk: async () => {
                await deleteApplicationAsync(id);
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
            {loading && appList.length === 0 ? (
                <div style={{ padding: 20 }}>
                    <Loading spinning />
                </div>
            ) : (
                <>
                    <List
                        dataSource={appList}
                        renderItem={(app) => (
                            <List.Item
                                key={app.id}
                                onClick={() => navigate(`/developer/settings/oauth2/application/${app.id}`)}
                                style={{ cursor: "pointer", padding: "12px 0" }}
                            >
                                <div style={{ width: "100%" }}>
                                    <OAuth2ClientApplicationItem
                                        oauth2App={app}
                                        onDelete={(id, e) => handleDeleteApplication(id, e)}
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

                    {noMore && appList.length > PAGE_SIZE && (
                        <div style={{ textAlign: "center", padding: 12, color: token.colorTextTertiary }}>
                            没有更多数据了
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default OAuth2ClientApplicationList;