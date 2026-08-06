import React, { useMemo, useRef, useState } from 'react';
import { Card, Divider, Flex, List, Segmented, Spin, Statistic, Tooltip, Typography, theme } from 'antd';
import {
  ExclamationCircleOutlined,
  UserOutlined,
  KeyOutlined,
  SyncOutlined,
  HistoryOutlined,
  CaretUpFilled,
  CaretDownFilled
} from '@ant-design/icons';
import { Column, Pie, Tiny } from '@ant-design/plots';
import './index.css';
import { useDomain } from '../../../router/DomainProvider';
import { getChartTrend, getGrantTypeStats, getOverviewAndTrend } from '../../../services/ProjectService';
import { useRequest } from 'ahooks';
import Loading from '../../../components/loading';

const { Text } = Typography;


// 趋势图数据映射
const formatTrendData = (trendList) => {
  if (!Array.isArray(trendList)) return [];
  return trendList.map((item, index) => {
    const rawVal = typeof item === 'object' && item !== null ? item.value : item;
    const numValue = Number(rawVal);
    const validValue = Number.isNaN(numValue) ? 0 : numValue;
    return {
      index: typeof item === 'object' && item?.index !== undefined ? item.index : index,
      value: validValue,
      displayValue: validValue + 0.1
    };
  });
};

const authTabList = [
  {
    key: 'ACTIVE_USER',
    label: '活跃用户',
  },
  {
    key: 'AUTH_COUNT',
    label: '授权次数',
  }
]

const ProjectOverview = () => {
  const { domainId } = useDomain()

  const { token } = theme.useToken()

  const rootRef = useRef()

  const [authActiveTabKey, setAuthActiveTabKey] = useState('ACTIVE_USER')


  const { data: overviewAndTrendData, loading: overviewAndTrendLoading } = useRequest(
    () => getOverviewAndTrend(domainId),
    {
      refreshDeps: [domainId],
      ready: !!domainId
    }
  )

  const { data: chartTrendData, loading: chartTrendLoading } = useRequest(
    () => getChartTrend(domainId, authActiveTabKey),
    {
      refreshDeps: [domainId, authActiveTabKey],
      ready: !!domainId && !!authActiveTabKey
    }
  )

  const { data: grantTypeStatsData, loading: grantTypeStatsLoading } = useRequest(
    () => getGrantTypeStats(domainId),
    {
      refreshDeps: [domainId],
      ready: !!domainId
    }
  )

  const [grantTypeKey, setGrantTypeKey] = useState(() => {
    return grantTypeStatsData?.length > 0 ? grantTypeStatsData[0].applicationName : 'all'
  })

  const {
    totalUserValue,
    totalActiveUserValue,
    totalActiveSessionValue,
    totalHistoryAuthorizationValue,
    totalUserTrend,
    totalActiveUserTrend,
    totalActiveSessionTrend,
    totalHistoryAuthorizationTrend
  } = useMemo(() => {
    const totalUserValue = Number(overviewAndTrendData?.totalUser?.value ?? 0)
    const totalActiveUserValue = Number(overviewAndTrendData?.activeUser?.value ?? 0)
    const totalActiveSessionValue = Number(overviewAndTrendData?.activeSession?.value ?? 0)
    const totalHistoryAuthorizationValue = Number(overviewAndTrendData?.historyAuthorization?.value ?? 0)
    return {
      // 指标的核心数值
      totalUserValue,
      totalActiveUserValue,
      totalActiveSessionValue,
      totalHistoryAuthorizationValue,

      // 趋势
      totalUserTrend: formatTrendData(overviewAndTrendData?.totalUser?.trend),
      totalActiveUserTrend: formatTrendData(overviewAndTrendData?.activeUser?.trend),
      totalActiveSessionTrend: formatTrendData(overviewAndTrendData?.activeSession?.trend),
      totalHistoryAuthorizationTrend: formatTrendData(overviewAndTrendData?.historyAuthorization?.trend),
    }
  }, [overviewAndTrendData])

  const { trendList, rankingList } = useMemo(() => {
    const trend = chartTrendData?.trendList?.map(item => {
      const value = Number(item.frequency ?? 0)
      return {
        ...item,
        letter: item.letter?.slice(5),
        frequency: value,
        displayFrequency: value === 0 ? 0.1 : value,
      };
    }) ?? []

    const ranking = chartTrendData?.rankingList?.map(item => {
      const score = Number(item.score ?? 0)
      return {
        ...item,
        label: item.applicationName,
        score: score,
      };
    }) ?? []

    return { trendList: trend, rankingList: ranking }
  }, [chartTrendData])

  const { grantTypeMap, grantTypeOptions } = useMemo(() => {
    const map = {}
    const options = []
    if (!Array.isArray(grantTypeStatsData)) {
      return {
        grantTypeMap: map,
        grantTypeOptions: options,
      }
    }
    grantTypeStatsData.forEach((item) => {
      if (!item?.applicationName) {
        return
      }
      const list = Array.isArray(item.grantTypes)
        ? item.grantTypes.map((g) => ({
          type: g?.grantType || '未知类型',
          value: Number(g?.count) || 0,
        }))
        :
        []
      map[item.applicationName] = list
      options.push({
        label: item.applicationName === 'all' ? '全部' : item.applicationName,
        value: item.applicationName,
      })
    })
    return {
      grantTypeMap: map,
      grantTypeOptions: options,
    }
  }, [grantTypeStatsData])


  const handleAuthActiveTabChange = (key) => {
    setAuthActiveTabKey(key)
  }

  const handleChannelGrantType = (value) => {
    setGrantTypeKey(value)
  }

  return (
    <Flex ref={rootRef} className="home-root-flex" gap={20} flex={1} vertical>

      <Flex gap={20}>
        {/* 1. 总用户数 */}
        <Card
          title="总用户"
          loading={overviewAndTrendLoading}
          style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Flex flex={1} vertical>
            <Flex style={{ height: '100px' }} vertical justify="space-between">
              <Statistic
                title={
                  <Flex justify="space-between" align="center">
                    <span>总用户数</span>
                    <Tooltip title="累计产生过授权的独立用户总数">
                      <Typography.Text type="secondary">
                        <ExclamationCircleOutlined />
                      </Typography.Text>
                    </Tooltip>
                  </Flex>
                }
                value={totalUserValue}
              />
              <Tiny.Area
                data={totalUserTrend}
                shapeField="smooth"
                xField="index"
                yField="displayValue"
                style={{
                  fill: 'linear-gradient(-90deg, white 0%, darkgreen 100%)',
                  fillOpacity: 0.6
                }}
                tooltip={{
                  title: false,
                  items: [
                    {
                      name: '总用户数',
                      field: 'value'
                    }
                  ]
                }}
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
          loading={overviewAndTrendLoading}
          style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Flex flex={1} vertical>
            <Flex style={{ height: '100px' }} vertical justify="space-between">
              <Statistic
                title={
                  <Flex justify="space-between" align="center">
                    <span>活跃用户数</span>
                    <Tooltip title="当前拥有未过期 Access Token 的独立用户数">
                      <Typography.Text type="secondary">
                        <ExclamationCircleOutlined />
                      </Typography.Text>
                    </Tooltip>
                  </Flex>
                }
                value={totalActiveUserValue}
              />
              <Tiny.Column
                data={totalActiveUserTrend}
                xField="index"
                yField="displayValue"
                style={{ fill: '#fa8c16' }}
                tooltip={{
                  title: false,
                  items: [
                    {
                      name: '活跃用户数',
                      field: 'value'
                    }
                  ]
                }}
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
          loading={overviewAndTrendLoading}
          style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Flex flex={1} vertical>
            <Flex style={{ height: '100px' }} vertical justify="space-between">
              <Statistic
                title={
                  <Flex justify="space-between" align="center">
                    <span>活跃会话数</span>
                    <Tooltip title="当前未过期的 Refresh Token 总数（长效会话数）">
                      <Typography.Text type="secondary">
                        <ExclamationCircleOutlined />
                      </Typography.Text>
                    </Tooltip>
                  </Flex>
                }
                value={totalActiveSessionValue}
              />
              <Tiny.Area
                data={totalActiveSessionTrend}
                shapeField="smooth"
                xField="index"
                yField="displayValue"
                style={{
                  fill: 'linear-gradient(-90deg, white 0%, darkgreen 100%)',
                  fillOpacity: 0.6
                }}
                tooltip={{
                  title: false,
                  items: [
                    {
                      name: '活跃会话数',
                      field: 'value'
                    }
                  ]
                }}
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
          loading={overviewAndTrendLoading}
          style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Flex flex={1} vertical>
            <Flex style={{ height: '100px' }} vertical justify="space-between">
              <Statistic
                title={
                  <Flex justify="space-between" align="center">
                    <span>累计授权数</span>
                    <Tooltip title="当前应用产生的所有 OAuth2 授权记录总数">
                      <Typography.Text type="secondary">
                        <ExclamationCircleOutlined />
                      </Typography.Text>
                    </Tooltip>
                  </Flex>
                }
                value={totalHistoryAuthorizationValue}
              />
              <Tiny.Line
                data={totalHistoryAuthorizationTrend}
                xField="index"
                yField="displayValue"
                smooth={true}
                style={{
                  stroke: '#13c2c2',
                  lineWidth: 2
                }}
                tooltip={{
                  title: false,
                  items: [
                    {
                      name: '累计授权数',
                      field: 'value'
                    }
                  ]
                }}
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

      <Flex>
        <Card
          style={{ width: '100%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
          tabList={authTabList}
          activeTabKey={authActiveTabKey}
          onTabChange={handleAuthActiveTabChange}
          tabProps={{
            size: 'middle',
          }}
          loading={chartTrendLoading}
        >
          <Flex justify='space-between'>
            <Flex style={{ minWidth: '0px' }} flex={8}>
              <div style={{ width: '100%' }}>
                <Column
                  data={trendList}
                  height={300}
                  xField='letter'
                  yField='displayFrequency'
                  label={{
                    text: (d) => d.frequency,
                    textBaseline: 'bottom',
                  }}
                  axis={{
                    x: {
                      style: {
                        labelFill: token.colorText,
                      },
                    },
                    y: {
                      style: {
                        labelFill: token.colorText,
                      },
                      labelFormatter: '~s',
                    },
                  }}
                  tooltip={{
                    title: false,
                    items: [
                      {
                        name: authActiveTabKey === 'ACTIVE_USER' ? '活跃用户' : '授权次数',
                        field: 'frequency'
                      }
                    ]
                  }}
                />
              </div>
            </Flex>
            <Flex style={{ paddingLeft: '20px' }} flex={2} vertical>
              <RankingList title='应用授权排名' data={rankingList} />
            </Flex>
          </Flex>
        </Card>
      </Flex>

      <Flex gap={25}>
        <Flex style={{ minWidth: '0px' }} flex={1}>
          <Card
            style={{ width: '100%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
            loading={grantTypeStatsLoading}
            title={
              <Flex justify='space-between'>
                <span>授权类别占比</span>
                <Segmented
                  style={{ fontWeight: 'normal' }}
                  options={grantTypeOptions}
                  value={grantTypeKey}
                  onChange={handleChannelGrantType}
                />
              </Flex>
            }
          >
            <Flex vertical>
              <Pie
                data={grantTypeMap?.[grantTypeKey] || []}
                angleField='value'
                colorField='type'
                radius={0.8}
                height={350}
                label={
                  {
                    text: (d) => `${d?.type}: ${d?.value}`,
                    position: 'spider',
                    style: {
                      fill: token.colorText,
                      connectorStroke: token.colorText,
                    },
                  }
                }
                legend={false}
              />
            </Flex>
          </Card>
        </Flex>
      </Flex>

    </Flex>
  )
};


const RankingList = ({ title, data }) => (
  <List
    size='small'
    className='ranking-list'
    header={(
      <Flex style={{ padding: '0 16px' }}>
        <Typography.Text strong>{title}</Typography.Text>
      </Flex>
    )}
    style={{ width: '80%' }}
    dataSource={data}
    renderItem={(item, index) => (
      <List.Item style={{ borderBottom: 'none' }}>
        <Flex flex={1} justify='space-between'>
          <Flex>
            <Typography.Text className={index <= 2 ? 'rank-circle-top' : 'rank-circle'}>{index + 1}</Typography.Text>
            <Typography.Text style={{ flex: 1 }}>{item.label}</Typography.Text>
          </Flex>
          <Flex>
            <Typography.Text>{item.score.toLocaleString()}</Typography.Text>
          </Flex>
        </Flex>
      </List.Item>
    )}
  />
)

export default ProjectOverview;