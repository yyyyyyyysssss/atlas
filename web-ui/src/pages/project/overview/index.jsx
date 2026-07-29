import React, { useRef } from 'react';
import { Card, Divider, Flex, Spin, Statistic, Tooltip, Typography, theme } from 'antd';
import {
  ExclamationCircleOutlined,
  UserOutlined,
  KeyOutlined,
  SyncOutlined,
  HistoryOutlined,
  CaretUpFilled,
  CaretDownFilled
} from '@ant-design/icons';
import { Tiny } from '@ant-design/plots';
import './index.css';
import { useDomain } from '../../../router/DomainProvider';
import { getOverviewAndTrend } from '../../../services/ProjectService';
import { useRequest } from 'ahooks';

const { Text } = Typography;

const ProjectOverview = () => {
  const { domainId } = useDomain();
  const { token } = theme.useToken();
  const rootRef = useRef();

  const {
    data: overviewAndTrendData,
    loading: overviewAndTrendLoading,
  } = useRequest(
    () => getOverviewAndTrend(domainId),
    {
      refreshDeps: [domainId],
      ready: !!domainId
    }
  );

  // 从真实接口获取 4 个指标的核心数值（转为数字，做安全回退 0 处理）
  const totalUserValue = Number(overviewAndTrendData?.totalUser?.value ?? 0);
  const totalActiveUserValue = Number(overviewAndTrendData?.activeUser?.value ?? 0);
  const totalActiveSessionValue = Number(overviewAndTrendData?.activeSession?.value ?? 0);
  const totalHistoryAuthorizationValue = Number(overviewAndTrendData?.historyAuthorization?.value ?? 0);

  // 1. 人均会话：活跃会话数 / 活跃用户数
  const avgTokensPerUser = totalActiveUserValue > 0
    ? (totalActiveSessionValue / totalActiveUserValue).toFixed(2)
    : '0';

  // 2. 长效会话覆盖率：活跃会话数 / 活跃用户数
  const tokenToRefreshRatio = totalActiveUserValue > 0
    ? ((totalActiveSessionValue / totalActiveUserValue) * 100).toFixed(1)
    : '0';

  // 3. 历史授权留存：当前活跃会话数 / 历史累计授权数
  const refreshRetentionRate = totalHistoryAuthorizationValue > 0
    ? ((totalActiveSessionValue / totalHistoryAuthorizationValue) * 100).toFixed(1)
    : '0';

  // 4. 当前活跃授权占比：当前活跃用户数 / 历史累计授权数
  const activeAuthRatio = totalHistoryAuthorizationValue > 0
    ? ((totalActiveUserValue / totalHistoryAuthorizationValue) * 100).toFixed(1)
    : '0';

  // 趋势图数据映射（兼容纯数组 ["4", "4"] 和 对象数组 [{ value: "4" }]）
  const formatTrendData = (trendList) => {
    if (!Array.isArray(trendList)) return [];
    return trendList.map((item, index) => {
      // 兼容 item 是 "4" 或 { index: 0, value: "4" }
      const rawVal = typeof item === 'object' && item !== null ? item.value : item;
      const numValue = Number(rawVal);
      return {
        index: typeof item === 'object' && item?.index !== undefined ? item.index : index,
        value: Number.isNaN(numValue) ? 0 : numValue
      };
    });
  };

  const totalUserTrend = formatTrendData(overviewAndTrendData?.totalUser?.trend);
  const totalActiveUserTrend = formatTrendData(overviewAndTrendData?.activeUser?.trend);
  const totalActiveSessionTrend = formatTrendData(overviewAndTrendData?.activeSession?.trend);
  const totalHistoryAuthorizationTrend = formatTrendData(overviewAndTrendData?.historyAuthorization?.trend);

  return (
    <Spin spinning={overviewAndTrendLoading}>
      <Flex ref={rootRef} className="home-root-flex" gap={20} flex={1} vertical style={{ padding: 24 }}>
        <Flex gap={20}>
          {/* 1. 总用户数 */}
          <Card
            title="总用户"
            style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
          >
            <Flex flex={1} vertical>
              <Flex style={{ height: '100px' }} vertical justify="space-between">
                <Statistic
                  title={
                    <Flex justify="space-between" align="center">
                      <span>总用户数</span>
                      <Tooltip title="累计产生过授权的独立用户总数">
                        <ExclamationCircleOutlined style={{ color: token.colorTextSecondary }} />
                      </Tooltip>
                    </Flex>
                  }
                  value={totalUserValue}
                />
                <Tiny.Area
                  data={totalUserTrend}
                  shapeField="smooth"
                  xField="index"
                  yField="value"
                  style={{
                    fill: 'linear-gradient(-90deg, white 0%, darkgreen 100%)',
                    fillOpacity: 0.6
                  }}
                  tooltip={{ title: '' }}
                />
              </Flex>

              <Divider style={{ margin: '10px 0' }} />

              <Flex gap="middle" wrap="nowrap">
                <div style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>
                  周环比 {overviewAndTrendData?.totalUser?.weekGrowth}
                  {
                    overviewAndTrendData?.totalUser?.weekPositive === true
                      ? <CaretUpFilled style={{ color: '#f5222d', marginRight: 8 }} />
                      : <CaretDownFilled style={{ color: '#52c41a' }} />
                  }
                </div>
                <div style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>
                  日环比 {overviewAndTrendData?.totalUser?.dayGrowth}
                  {
                    overviewAndTrendData?.totalUser?.dayPositive === true
                      ? <CaretUpFilled style={{ color: '#f5222d', marginRight: 8 }} />
                      : <CaretDownFilled style={{ color: '#52c41a' }} />
                  }
                </div>
              </Flex>
            </Flex>
          </Card>

          {/* 2. 活跃用户数 */}
          <Card
            title="活跃用户"
            style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
          >
            <Flex flex={1} vertical>
              <Flex style={{ height: '100px' }} vertical justify="space-between">
                <Statistic
                  title={
                    <Flex justify="space-between" align="center">
                      <span>活跃用户数</span>
                      <Tooltip title="当前拥有未过期 Access Token 的独立用户数">
                        <ExclamationCircleOutlined style={{ color: token.colorTextSecondary }} />
                      </Tooltip>
                    </Flex>
                  }
                  value={totalActiveUserValue}
                />
                <Tiny.Column
                  data={totalActiveUserTrend}
                  xField="index"
                  yField="value"
                  style={{ fill: '#fa8c16' }}
                  tooltip={{ title: '' }}
                />
              </Flex>

              <Divider style={{ margin: '10px 0' }} />

              <Flex gap={8} align='center'>
                <span style={{ fontSize: 14 }}>活跃率</span>
                <span style={{ fontSize: 14 }}>{overviewAndTrendData?.activeUser?.activeRate}</span>
              </Flex>

            </Flex>
          </Card>

          {/* 3. 活跃会话数 (Refresh Token) */}
          <Card
            title="活跃会话"
            style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
          >
            <Flex flex={1} vertical>
              <Flex style={{ height: '100px' }} vertical justify="space-between">
                <Statistic
                  title={
                    <Flex justify="space-between" align="center">
                      <span>活跃会话数</span>
                      <Tooltip title="当前未过期的 Refresh Token 总数（长效会话数）">
                        <ExclamationCircleOutlined style={{ color: token.colorTextSecondary }} />
                      </Tooltip>
                    </Flex>
                  }
                  value={totalActiveSessionValue}
                />
                <Tiny.Area
                  data={totalActiveSessionTrend}
                  shapeField="smooth"
                  xField="index"
                  yField="value"
                  style={{
                    fill: 'linear-gradient(-90deg, white 0%, darkgreen 100%)',
                    fillOpacity: 0.6
                  }}
                  tooltip={{ title: '' }}
                />
              </Flex>

              <Divider style={{ margin: '10px 0' }} />

              <Flex gap={8} align='center'>
                <span style={{ fontSize: 14 }}>人均会话</span>
                <span style={{ fontSize: 14 }}>{overviewAndTrendData?.activeSession?.avgSessionPerUser}</span>
              </Flex>


            </Flex>
          </Card>

          {/* 4. 历史累计授权次数 */}
          <Card
            title="累计授权"
            style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
          >
            <Flex flex={1} vertical>
              <Flex style={{ height: '100px' }} vertical justify="space-between">
                <Statistic
                  title={
                    <Flex justify="space-between" align="center">
                      <span>累计授权数</span>
                      <Tooltip title="当前应用产生的所有 OAuth2 授权记录总数">
                        <ExclamationCircleOutlined style={{ color: token.colorTextSecondary }} />
                      </Tooltip>
                    </Flex>
                  }
                  value={totalHistoryAuthorizationValue}
                />
                <Tiny.Line
                  data={totalHistoryAuthorizationTrend}
                  xField="index"
                  yField="value"
                  smooth={true}
                  style={{
                    stroke: '#13c2c2',
                    lineWidth: 2
                  }}
                  tooltip={{ title: '' }}
                />
              </Flex>

              <Divider style={{ margin: '10px 0' }} />

              <Flex gap={8} align='center'>
                <span style={{ fontSize: 14 }}>近7日新增</span>
                <span style={{ fontSize: 14 }}>{overviewAndTrendData?.historyAuthorization?.lastWeekTotalAuthorizationCount}</span>
              </Flex>
            </Flex>
          </Card>
        </Flex>
      </Flex>
    </Spin>
  );
};

export default ProjectOverview;