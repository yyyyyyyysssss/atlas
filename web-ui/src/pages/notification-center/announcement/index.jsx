import React, { useEffect, useState } from 'react';
import { Button, Col, Flex, Form, Input, Modal, Row, Select, Space, Tag, theme, Tooltip, Typography } from 'antd';
import { useRequest } from 'ahooks';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { EditOutlined, EyeOutlined } from '@ant-design/icons';
import HasPermission from '../../../components/HasPermission';
import SmartTable from '../../../components/smart-table';
import { OperationMode } from '../../../enums/common';
import './index.css';
import { AnnouncementStatus, AnnouncementType } from '../../../enums/notification';
import { fetchAnnouncementList, getAnnouncementDetails } from '../../../services/NotificationService';
import OptionSelect from '../../../components/OptionSelect';
import AnnouncementDetailModal from './components/AnnouncementDetailModal';

const { Text } = Typography;

const NotificationAnnouncement = () => {
  const { t } = useTranslation()

  const navigate = useNavigate()


  const [searchForm] = Form.useForm()

  const [selectedId, setSelectedId] = useState(null)

  const [queryParam, setQueryParam] = useState({
    pageNum: 1,
    pageSize: 10,
    title: null,
    type: null,
    status: null,
  });

  // API 请求 hooks
  const { runAsync: fetchListAsync, loading: listLoading } = useRequest(fetchAnnouncementList, { manual: true })

  const getData = async (params) => {
    return await fetchListAsync(params);
  };

  const handleSearch = () => {
    searchForm.validateFields().then(values => {
      const newQueryParam = { ...queryParam, ...values, pageNum: 1 };
      setQueryParam(newQueryParam);
    });
  };

  const handleReset = () => {
    searchForm.resetFields();
    setQueryParam({ pageNum: 1, pageSize: 10, keyword: null, type: null, status: null });
  };

  const handleRefresh = () => {
    setQueryParam({ ...queryParam });
  };

  const handleAdd = () => {
    navigate('/notification/announcement/details', {
      state: {
        operationMode: OperationMode.ADD.value
      }
    });
  };

  const handleEdit = (id) => {
    navigate('/notification/announcement/details', {
      state: {
        id: id,
        operationMode: OperationMode.EDIT.value
      }
    });
  };

  const handleView = async (id) => {
    setSelectedId(id)
  };

  const columns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      align: 'center',
      visible: true,
      render: (text) => (
        <Text
          ellipsis={{
            tooltip: text,
          }}
          style={{ maxWidth: '200px' }}
        >
          {text}
        </Text>
      ),
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      align: 'center',
      visible: true,
      render: (type) => {
        const config = AnnouncementType[type]
        return config ? (
          <Tag color={config.color}>{config.label}</Tag>
        ) : (
          <Tag>{type}</Tag> // 如果后端传了未知类型，保底显示原始值
        )
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      align: 'center',
      visible: true,
      render: (status) => {
        const config = AnnouncementStatus[status]
        return config ? (
          <Tag color={config.color}>{config.label}</Tag>
        ) : (
          <Tag>{status}</Tag>
        )
      },
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: '80px',
      visible: true,
      align: 'center',
      render: (priority) => <Text strong>{priority}</Text>,
    },
    {
      title: '发布时间',
      dataIndex: 'publishTime',
      key: 'publishTime',
      align: 'center',
      visible: true,
    },
    {
      title: '发布人',
      dataIndex: 'creatorName',
      key: 'creatorName',
      align: 'center',
      visible: true,
    },
    {
      title: '操作',
      key: 'operation',
      visible: true,
      align: 'center',
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title={t('查看')}>
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleView(record.id)}
            />
          </Tooltip>
          <HasPermission hasPermissions='system:announcement:write'>
            <Tooltip title={t('编辑')}>
              <Button
                type="text"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEdit(record.id)}
              />
            </Tooltip>
          </HasPermission>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Flex vertical gap={16}>
        {/* 搜索框 */}
        <Flex justify="center" gap={8}>
          <Form form={searchForm} layout="horizontal">
            <Row gutter={[24, 16]} align="middle">
              <Col>
                <Form.Item name="title" label="公告标题" style={{ margin: 0 }}>
                  <Input placeholder="请输入标题" allowClear style={{ width: 200 }} />
                </Form.Item>
              </Col>
              <Col>
                <Form.Item name="type" label="类型" style={{ margin: 0 }}>
                  <OptionSelect
                    style={{ width: 150 }}
                    loadData={Object.values(AnnouncementType)}
                  />
                </Form.Item>
              </Col>
              <Col>
                <Form.Item name="status" label="状态" style={{ margin: 0 }}>
                  <OptionSelect
                    style={{ width: 150 }}
                    loadData={Object.values(AnnouncementStatus)}
                  />
                </Form.Item>
              </Col>
              <Col>
                <Space>
                  <Button type="primary" onClick={handleSearch} loading={listLoading}>
                    {t('查询')}
                  </Button>
                  <Button onClick={handleReset} loading={listLoading}>
                    {t('重置')}
                  </Button>
                </Space>
              </Col>
            </Row>
          </Form>
        </Flex>

        {/* 表格 */}
        <SmartTable
          columns={columns}
          headerExtra={
            <HasPermission hasPermissions='system:announcement:write'>
              <Button type="primary" onClick={handleAdd}>
                + 新建公告
              </Button>
            </HasPermission>
          }
          fetchData={getData}
          loading={listLoading}
          queryParam={queryParam}
          setQueryParam={setQueryParam}
          rowKey="id"
          scroll={{ x: 1200 }}
        />

      </Flex>
      <AnnouncementDetailModal id={selectedId} onCancel={() => setSelectedId(null)} />
    </>
  );
};

export default NotificationAnnouncement;
