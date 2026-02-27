import React, { useRef, useState } from 'react';
import './index.css'
import { Card, Divider, Flex, List, Progress, Segmented, Statistic, Table, Tooltip, Typography, theme } from 'antd';
import { ExclamationCircleOutlined, CaretUpFilled, CaretDownFilled } from '@ant-design/icons';
import { Column, Pie, Line } from '@ant-design/plots';
import { Tiny } from '@ant-design/plots';
import { format } from 'fecha';
import FullScreenButton from '../../components/FullScreenButton';


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
            <Typography.Text style={{ flex: 1 }}>{item.name}</Typography.Text>
          </Flex>
          <Flex>
            <Typography.Text>{item.value.toLocaleString()}</Typography.Text>
          </Flex>
        </Flex>
      </List.Item>
    )}
  />
)

const Home = () => {

  const { token } = theme.useToken()

  const rootRef = useRef()

  const visitDataset = [
    264, 417, 438, 887, 309, 397, 550, 575, 563, 430, 525, 592, 492, 467, 513, 546, 983, 340, 539, 243, 226, 192,
  ].map((value, index) => ({ value, index }))

  const paymentDataset = [
    264, 417, 438, 887, 309, 397, 550, 575, 563, 430, 525, 592, 492, 467, 513, 546, 983, 340, 539, 243, 226, 192,
  ].map((value, index) => ({ value, index }))

  const [saleVisitActiveTabKey, setSaleVisitActiveTabKey] = useState('sale')

  const [saleProportionKey, setSaleProportionKey] = useState('all')

  const saleVisitTabList = [
    {
      key: 'sale',
      label: '销售额',
    },
    {
      key: 'visit',
      label: '访问量',
    }
  ]

  const rankingData = [
    { name: '宝山路 0 号店', value: 323234 },
    { name: '宝山路 1 号店', value: 299132 },
    { name: '宝山路 2 号店', value: 283123 },
    { name: '宝山路 3 号店', value: 263123 },
    { name: '宝山路 4 号店', value: 253123 },
    { name: '宝山路 5 号店', value: 243123 },
    { name: '宝山路 6 号店', value: 193123 },
  ]

  const saleVisitcontentList = {
    sale:
      <Flex justify='space-between'>
        <Flex style={{ minWidth: '0px' }} flex={7}>
          <div style={{ width: '100%' }}>
            <Column
              data={{
                type: 'fetch',
                value: 'https://render.alipay.com/p/yuyan/180020010001215413/antd-charts/column-column.json',
              }}
              height={300}
              xField='letter'
              yField='frequency'
              label={{
                text: (d) => `${(d.frequency * 100).toFixed(1)}%`,
                textBaseline: 'bottom',
              }}
              axis={{
                y: {
                  labelFormatter: '.0%',
                },
              }}
              style={{

              }}
            />
          </div>
        </Flex>
        <Flex justify='center' flex={3} vertical>
          <RankingList title='门店销售排名' data={rankingData} />
        </Flex>
      </Flex>,
    visit:
      <Flex justify='space-between'>
        <Flex style={{ minWidth: '0px' }} flex={7}>
          <div style={{ width: '100%' }}>
            <Column
              data={{
                type: 'fetch',
                value: 'https://render.alipay.com/p/yuyan/180020010001215413/antd-charts/column-column.json',
              }}
              height={300}
              xField='letter'
              yField='frequency'
              label={{
                text: (d) => `${(d.frequency * 100).toFixed(1)}%`,
                textBaseline: 'bottom',
              }}
              axis={{
                y: {
                  labelFormatter: '.0%',
                },
              }}
              style={{

              }}
            />
          </div>
        </Flex>
        <Flex justify='center' flex={3} vertical>
          <RankingList title='门店访问排名' data={rankingData} />
        </Flex>
      </Flex>
  }

  const handleSaleVisitActiveTabChange = (key) => {
    setSaleVisitActiveTabKey(key)
  }

  const saleProportionMap = {
    all: [
      { type: '家用电器', value: 4544 },
      { type: '母婴产品', value: 1231 },
      { type: '服饰箱包', value: 2341 },
      { type: '个护健康', value: 3113 },
      { type: '食用酒水', value: 3321 },
      { type: '其他', value: 1231 }
    ],
    online: [
      { type: '家用电器', value: 3544 },
      { type: '母婴产品', value: 2231 },
      { type: '服饰箱包', value: 4341 },
      { type: '个护健康', value: 1113 },
      { type: '食用酒水', value: 2321 },
      { type: '其他', value: 2231 }
    ],
    store: [
      { type: '家用电器', value: 3544 },
      { type: '母婴产品', value: 4231 },
      { type: '服饰箱包', value: 1341 },
      { type: '个护健康', value: 5113 },
      { type: '食用酒水', value: 3321 },
      { type: '其他', value: 3231 }
    ]
  }

  const handleChannelSaleChange = (value) => {
    setSaleProportionKey(value)
  }

  const hotSearchColumns = [
    {
      title: '排名',
      dataIndex: 'ranking',
      key: 'ranking',
    },
    {
      title: '搜索关键词',
      dataIndex: 'keyword',
      key: 'keyword',
    },
    {
      title: '用户数',
      dataIndex: 'userCount',
      key: 'userCount',
      sorter: (a, b) => a.userCount - b.userCount,
    },
    {
      title: '周涨幅',
      dataIndex: 'weeklyIncrease',
      key: 'weeklyIncrease',
      sorter: (a, b) => a.weeklyIncrease - b.weeklyIncrease,
      render: (_, { weeklyIncrease }) => (
        <Flex gap={3}>
          <Typography.Text>
            {
              new Intl.NumberFormat('zh-CN', {
                style: 'percent',
                minimumFractionDigits: 0, // 保留两位小数
              }).format(weeklyIncrease)
            }
          </Typography.Text>
          <CaretUpFilled style={{ color: weeklyIncrease > 0.5 ? '#f5222d' : '#52c41a' }} />
        </Flex>
      )
    },
  ]

  const hotSearchData = [
    {
      key: '1',
      ranking: 1,
      keyword: '搜索关键词-0',
      userCount: 158,
      weeklyIncrease: '0.7'
    },
    {
      key: '2',
      ranking: 2,
      keyword: '搜索关键词-1',
      userCount: 479,
      weeklyIncrease: '0.99'
    },
    {
      key: '3',
      ranking: 3,
      keyword: '搜索关键词-2',
      userCount: 855,
      weeklyIncrease: '0.85'
    },
    {
      key: '4',
      ranking: 4,
      keyword: '搜索关键词-3',
      userCount: 993,
      weeklyIncrease: '0.04'
    },
    {
      key: '5',
      ranking: 5,
      keyword: '搜索关键词-4',
      userCount: 886,
      weeklyIncrease: '0.64'
    },
    {
      key: '6',
      ranking: 6,
      keyword: '搜索关键词-5',
      userCount: 204,
      weeklyIncrease: '0.32'
    },
  ]

  return (
    <Flex ref={rootRef} className='home-root-flex' gap={20} flex={1} vertical>
      <Flex justify='end'>
        <FullScreenButton targetRef={rootRef} />
      </Flex>
      <Flex gap={25}>
        {/* 总销售额 */}
        <Card
          title="总销售额"
          style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Flex flex={1} vertical>
            <Flex gap={20} style={{ height: '100px' }} vertical>
              <Statistic
                title={
                  <Flex justify='space-between'>
                    <span>总销售额</span>
                    <Tooltip title="指标说明">
                      <ExclamationCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                    </Tooltip>
                  </Flex>
                }
                value={126560}
                precision={2}
                prefix="¥"
              />
              <Flex>
                <div>
                  周同比 12% <CaretUpFilled style={{ color: '#f5222d', marginRight: 8 }} />
                </div>
                <div>
                  日同比 11% <CaretDownFilled style={{ color: '#52c41a' }} />
                </div>
              </Flex>
            </Flex>

            <Flex>
              <Divider style={{ margin: '0px', marginTop: '10px', marginBottom: '10px' }} />
            </Flex>

            <Flex gap={8} align='center'>
              <span style={{ fontSize: 14 }}>日销售额</span>
              <span style={{ fontSize: 14 }}>¥ 12,423</span>
            </Flex>

          </Flex>
        </Card>
        {/* 访问量 */}
        <Card
          title="访问量"
          style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Flex flex={1} vertical>
            <Flex style={{ height: '100px' }} vertical>
              <Statistic
                title={
                  <Flex justify='space-between'>
                    <span>访问量</span>
                    <Tooltip title="指标说明">
                      <ExclamationCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                    </Tooltip>
                  </Flex>
                }
                value={8846}
              />
              <Tiny.Area
                data={visitDataset}
                shapeField='smooth'
                xField='index'
                yField='value'
                style={
                  {
                    fill: 'linear-gradient(-90deg, white 0%, darkgreen 100%)',
                    fillOpacity: 0.6
                  }
                }
                tooltip={
                  {
                    title: '',
                  }
                }
              />
            </Flex>

            <Flex>
              <Divider style={{ margin: '0px', marginTop: '10px', marginBottom: '10px' }} />
            </Flex>

            <Flex gap={8} align='center'>
              <span style={{ fontSize: 14 }}>日访问量</span>
              <span style={{ fontSize: 14 }}>1,234</span>
            </Flex>

          </Flex>

        </Card>
        {/* 支付笔数 */}
        <Card
          title="支付笔数"
          style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Flex flex={1} vertical>
            <Flex style={{ height: '100px' }} vertical>
              <Statistic
                title={
                  <Flex justify='space-between'>
                    <span>支付笔数</span>
                    <Tooltip title="指标说明">
                      <ExclamationCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                    </Tooltip>
                  </Flex>
                }
                value={6560}
              />
              <Tiny.Column
                data={paymentDataset}
                xField='index'
                yField='value'
                style={
                  {}
                }
                tooltip={
                  {
                    title: '',
                  }
                }
              />
            </Flex>

            <Flex>
              <Divider style={{ margin: '0px', marginTop: '10px', marginBottom: '10px' }} />
            </Flex>

            <Flex gap={8} align='center'>
              <span style={{ fontSize: 14 }}>转化率</span>
              <span style={{ fontSize: 14 }}>60%</span>
            </Flex>

          </Flex>
        </Card>
        {/* 运营活动效果 */}
        <Card
          title="运营活动效果"
          style={{ width: '25%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Flex flex={1} vertical>
            <Flex style={{ height: '100px' }} vertical>
              <Statistic
                title={
                  <Flex justify='space-between'>
                    <span>运营活动效果</span>
                    <Tooltip title="指标说明">
                      <ExclamationCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                    </Tooltip>
                  </Flex>
                }
                value='78%'
              />
              <Progress
                percent={78}
                strokeColor={token.colorPrimary}
              />
            </Flex>

            <Flex>
              <Divider style={{ margin: '0px', marginTop: '10px', marginBottom: '10px' }} />
            </Flex>

            <Flex gap={8} align='center'>
              <div>
                周同比 12% <CaretUpFilled style={{ color: '#f5222d', marginRight: 8 }} />
              </div>
              <div>
                日同比 11% <CaretDownFilled style={{ color: '#52c41a' }} />
              </div>
            </Flex>

          </Flex>
        </Card>
      </Flex>

      {/* 销售额访问量柱状图以及门店销售排名 */}
      <Flex>
        <Card
          style={{ width: '100%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
          tabList={saleVisitTabList}
          activeTabKey={saleVisitActiveTabKey}
          onTabChange={handleSaleVisitActiveTabChange}
          tabProps={{
            size: 'middle',
          }}
        >
          {saleVisitcontentList[saleVisitActiveTabKey]}
        </Card>
      </Flex>

      {/* 热门搜索以及销售类比饼图 */}
      <Flex gap={25}>
        {/* 热门搜索 */}
        <Flex style={{ minWidth: '0px' }} flex={1}>
          <Card
            title="线上热门搜索"
            style={{ width: '100%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
          >
            <Flex flex={1} gap={20} vertical>
              <Flex gap={50} justify='space-between'>
                <Flex flex={1} style={{ height: '100px', minWidth: '0px' }} vertical>
                  <Statistic
                    title={
                      <Flex gap={5}>
                        <span>搜索用户数</span>
                        <Tooltip title="指标说明">
                          <ExclamationCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                        </Tooltip>
                      </Flex>
                    }
                    valueRender={() => (
                      <Flex gap={3}>
                        <Typography.Text style={{ fontSize: '18px' }} strong>1.71</Typography.Text>
                        <CaretUpFilled style={{ fontSize: '16px' }} />
                      </Flex>
                    )}
                  />
                  <Tiny.Area
                    data={visitDataset}
                    shapeField='smooth'
                    xField='index'
                    yField='value'
                    style={
                      {
                        fill: 'linear-gradient(-90deg, white 0%, darkgreen 100%)',
                        fillOpacity: 0.6,
                      }
                    }
                    tooltip={
                      {
                        title: '',
                      }
                    }
                  />
                </Flex>
                <Flex flex={1} style={{ height: '100px', minWidth: '0px' }} vertical>
                  <Statistic
                    title={
                      <Flex gap={5}>
                        <span>人均搜索次数</span>
                        <Tooltip title="指标说明">
                          <ExclamationCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                        </Tooltip>
                      </Flex>
                    }
                    valueRender={() => (
                      <Flex gap={3}>
                        <Typography.Text style={{ fontSize: '18px' }} strong>26.2</Typography.Text>
                        <CaretDownFilled style={{ fontSize: '16px' }} />
                      </Flex>
                    )}
                  />
                  <Tiny.Area
                    data={visitDataset}
                    shapeField='smooth'
                    xField='index'
                    yField='value'
                    style={
                      {
                        fill: 'linear-gradient(-90deg, white 0%, darkgreen 100%)',
                        fillOpacity: 0.6
                      }
                    }
                    tooltip={
                      {
                        title: '',
                      }
                    }
                  />
                </Flex>
              </Flex>

              <Flex>
                <Table
                  style={{
                    width: '100%',
                  }}
                  size="small"
                  columns={hotSearchColumns}
                  dataSource={hotSearchData}
                  pagination={{ pageSize: 5 }}
                />
              </Flex>

            </Flex>
          </Card>
        </Flex>
        {/* 销售类比 */}
        <Flex style={{ minWidth: '0px' }} flex={1}>
          <Card
            style={{ width: '100%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
            title={
              <Flex justify='space-between'>
                <span>销售额类别占比</span>
                <Segmented
                  style={{ fontWeight: 'normal' }}
                  options={[{ label: '全部渠道', value: 'all' }, { label: '线上', value: 'online' }, { label: '门店', value: 'store' }]}
                  onChange={handleChannelSaleChange}
                />
              </Flex>
            }
          >
            <Flex vertical>
              <Typography.Text>销售额</Typography.Text>
              <Pie
                data={saleProportionMap[saleProportionKey]}
                angleField='value'
                colorField='type'
                radius={0.8}
                height={350}
                label={
                  {
                    text: (d) => `${d.type}: ${d.value}`,
                    position: 'spider',
                  }
                }
                legend={false}
              />
            </Flex>
          </Card>
        </Flex>
      </Flex>

      {/* 支付笔数折线图 */}
      <Flex flex={1} style={{marginBottom: '10px'}}>
        <Card
          title='支付笔数'
          style={{ width: '100%', boxShadow: 'var(--ant-box-shadow-tertiary)' }}
        >
          <Line
            data={{
              type: 'fetch',
              value: 'https://render.alipay.com/p/yuyan/180020010001215413/antd-charts/line-slider.json',
            }}
            xField={(d) => new Date(d.date)}
            yField='close'
            legend={{ position: 'top' }}
            axis={{ x: { title: false, size: 40 }, y: { title: false, size: 36 } }}
            slider={
              {
                x: { labelFormatter: (d) => format(d, 'YYYY/M/D') },
              }
            }
          />
        </Card>
      </Flex>
    </Flex>
  )
}

export default Home