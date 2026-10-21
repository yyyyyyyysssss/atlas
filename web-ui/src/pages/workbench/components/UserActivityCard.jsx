import React, { useRef } from 'react';
import { Card, Flex, Typography, Timeline, Button, theme, Tag } from 'antd';
import { History } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import useRequest from 'ahooks/lib/useRequest';
import NoDataEmpty from '../../../components/NoDataEmpty';
import { useInfiniteScroll } from 'ahooks';
import { getAuditLogPage } from '../../../services/AuditLogService';
import Loading from '../../../components/loading';
import { formatRelativeTime } from '../../../utils/format';


const { Text, Title, Paragraph } = Typography;

const PAGE_SIZE = 10;

const UserActivityCard = ({ limit = 10 }) => {

    const { token } = theme.useToken()

    const navigate = useNavigate()

    const scrollRef = useRef(null)

    const {
        data,
        loading,
        loadingMore,
        noMore
    } = useInfiniteScroll(
        async (d) => {
            const pageNum = d ? d.nextPage : 1;
            const res = await getAuditLogPage(pageNum, PAGE_SIZE);
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

    const auditLogs = data?.list || [];

    return (
        <Card
            title={
                <Flex align="center" gap={8}>
                    <History size={18} color={token.colorPrimary} />
                    <span>我的动态</span>
                </Flex>
            }
            variant="borderless"
            style={{ height: '100%' }}
        >
            {/* 3. 给滚动区域设置容器 ref 和固定高度/overflow */}
            <div
                ref={scrollRef}
                className="hover-scrollbar"
                style={{
                    height: 350, // 设定容器可视高度
                    overflow: 'auto',
                    paddingRight: 8
                }}
            >
                {/* 初次加载中状态 */}
                {loading && auditLogs.length === 0 ? (
                    <Flex justify="center" align="center" style={{ minHeight: 200, height: '100%' }}>
                        <Loading spinning size="small" />
                    </Flex>
                ) : auditLogs.length > 0 ? (
                    <>
                        <Timeline
                            style={{ marginTop: 8 }}
                            items={auditLogs.map((log, index) => ({
                                key: log.id || index,
                                color: token.colorPrimary,
                                children: (
                                    <Flex vertical gap={2}>
                                        <Flex justify="space-between" align="center">
                                            <Text strong style={{ color: token.colorTextHeading, fontSize: 13 }}>
                                                {log.summary}
                                            </Text>
                                            <Text type="secondary" style={{ fontSize: 12, flexShrink: 0 }}>
                                                {formatRelativeTime(log.createTime)}
                                            </Text>
                                        </Flex>
                                        <Flex align="center" gap={4} style={{ marginTop: 2 }}>
                                            <Text
                                                style={{
                                                    fontSize: 12,
                                                    color: token.colorTextDescription,
                                                    backgroundColor: token.colorFillQuaternary,
                                                    padding: '1px 8px',
                                                    borderRadius: token.borderRadiusLG,
                                                    maxWidth: '180px',
                                                    display: 'inline-block',
                                                }}
                                                ellipsis={{ tooltip: log.target }}
                                            >
                                                {log.target}
                                            </Text>
                                        </Flex>
                                    </Flex>
                                ),
                            }))}
                        />

                        {/* 加载更多时的底部 loading */}
                        {loadingMore && (
                            <div style={{ textAlign: 'center', padding: '12px 0' }}>
                                <Loading spinning size="small" />
                            </div>
                        )}

                        {/* 没有更多数据提示 */}
                        {noMore && auditLogs.length > PAGE_SIZE && (
                            <div style={{ textAlign: 'center', padding: '12px 0', color: token.colorTextTertiary, fontSize: 12 }}>
                                没有更多数据了
                            </div>
                        )}
                    </>
                ) : (
                    <Flex justify="center" align="center" style={{ minHeight: 200, height: '100%' }}>
                        <NoDataEmpty description="暂无动态" />
                    </Flex>
                )}
            </div>
        </Card>
    );
};

export default UserActivityCard;