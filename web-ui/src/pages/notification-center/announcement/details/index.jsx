import React, { useEffect, useState } from 'react';
import { Button, Col, Flex, Form, Input, Row, Space, theme, Divider, Select, InputNumber, DatePicker, Card, Typography } from 'antd';
import useFullParams from '../../../../hooks/useFullParams';
import { useTranslation } from 'react-i18next';
import useBack from '../../../../hooks/useBack';
import { useRequest } from 'ahooks';
import { OperationMode } from '../../../../enums/common';
import { getMessageApi } from '../../../../utils/MessageUtil';
import Loading from '../../../../components/loading';
import dayjs from 'dayjs';
import OptionSelect from '../../../../components/OptionSelect';
import { AnnouncementStatus, AnnouncementType } from '../../../../enums/notification';
import { createAnnouncement, getAnnouncementDetails, updateAnnouncement } from '../../../../services/NotificationService';
import AnnouncementMarkdownView from '../components/AnnouncementMarkdownView';

const { Text, Paragraph, Title } = Typography;

const AnnouncementDetails = () => {
  const { t } = useTranslation();
  const { token } = theme.useToken();
  const { announcementId, operationMode = OperationMode.ADD.value } = useFullParams();
  const [form] = Form.useForm();
  const { goBack } = useBack();
  const [contentValue, setContentValue] = useState('');

  const { runAsync: createAsync, loading: createLoading } = useRequest(createAnnouncement, { manual: true });
  const { runAsync: updateAsync, loading: updateLoading } = useRequest(updateAnnouncement, { manual: true });
  const { runAsync: detailAsync, loading: detailLoading } = useRequest(getAnnouncementDetails, { manual: true });

  const fetchData = async (id) => {
    if (!id) return;
    form.resetFields();
    const data = await detailAsync(id);
    form.setFieldsValue({
      ...data,
      publishTime: data.publishTime ? dayjs(data.publishTime) : null,
    });
    setContentValue(data.content || '');
  };

  useEffect(() => {
    switch (operationMode) {
      case OperationMode.EDIT.value:
        fetchData(announcementId);
        break;
      case OperationMode.ADD.value:
        form.resetFields();
        form.setFieldsValue({
          status: 'PUBLISHED',
          priority: 10,
        });
        setContentValue('');
        break;
    }
  }, [operationMode, announcementId]);

  const saveAnnouncement = async () => {
    const values = await form.validateFields()
    const params = {
      ...values,
      // 这里的 .format() 必须和你的 DatePicker 格式一致
      publishTime: values.publishTime ? values.publishTime.format('YYYY-MM-DD HH:mm:ss') : undefined,
    };
    if (operationMode === OperationMode.ADD.value) {
      await createAsync(params);
      getMessageApi().success(t('创建成功'));
    } else {
      await updateAsync(params);
      getMessageApi().success(t('修改成功'));
    }
    goBack();
  };

  const handleFormValuesChange = (changedValues) => {
    if (changedValues.content !== undefined) {
      setContentValue(changedValues.content || '');
    }
  };

  return (
    <Loading spinning={detailLoading || createLoading || updateLoading}>
      <Flex
        gap={20}
        style={{ height: '100%', padding: '20px', background: token.colorBgContainer }}
        vertical={false}
      >
        {/* 左侧编辑表单 */}
        <Flex
          vertical
          style={{
            minHeight: '100vh',
            overflowY: 'auto',
            paddingRight: '10px',
            flex: '0 0 50%',
          }}
        >
          <Form
            form={form}
            layout="vertical"
            onValuesChange={handleFormValuesChange}
          >
            <Form.Item name="id" hidden>
              <Input />
            </Form.Item>

            <Form.Item
              label="公告标题"
              name="title"
              rules={[{ required: true, message: '请输入公告标题' }]}
            >
              <Input placeholder="请输入公告标题" />
            </Form.Item>

            <Form.Item
              label="摘要"
              name="description"
              rules={[{ required: true, message: '请输入摘要' }]}
            >
              <Input.TextArea placeholder="请输入摘要" rows={2} />
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="公告类型"
                  name="type"
                  rules={[{ required: true, message: '请选择类型' }]}
                >
                  <OptionSelect
                    loadData={Object.values(AnnouncementType)}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="版本号" name="version">
                  <Input placeholder="例如: v2.6.0（可选）" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="优先级"
                  name="priority"
                  rules={[{ required: true, message: '请输入优先级' }]}
                >
                  <InputNumber placeholder="数字越大越靠前" min={0} max={100} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="状态"
                  name="status"
                  rules={[{ required: true, message: '请选择状态' }]}
                >
                  <OptionSelect
                    loadData={Object.values(AnnouncementStatus)}
                  />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              label="内容（Markdown格式）"
              name="content"
              rules={[{ required: true, message: '请输入内容' }]}
            >
              <Input.TextArea
                placeholder="支持 Markdown 格式，右侧实时预览"
                rows={15}
                style={{ fontFamily: 'monospace', fontSize: 13 }}
              />
            </Form.Item>
          </Form>

          {/* 保存按钮 */}
          <Flex justify="flex-end" gap={8} style={{ marginTop: '20px' }}>
            <Button onClick={() => goBack()}>
              {t('取消')}
            </Button>
            <Button type="primary" loading={createLoading || updateLoading} onClick={saveAnnouncement}>
              {t('保存')}
            </Button>
          </Flex>
        </Flex>

        {/* 分隔线 */}
        <Divider type="vertical" style={{ height: '100vh', margin: 0 }} />

        {/* 右侧预览 */}
        <Flex
          vertical
          style={{
            minHeight: '100vh',
            paddingLeft: '10px',
            flex: '0 0 50%',
          }}
        >
          <Card
            title={<Text strong>Markdown 预览</Text>}
            variant="borderless"
            style={{ height: '100%' }}
            styles={{
              body: {
                padding: '16px',
                height: '100vh',
                overflowY: 'auto'
              }
            }}
          >
            {contentValue ? (
              <AnnouncementMarkdownView
                content={contentValue}
              />
            ) : (
              <Text type="secondary">在左侧编辑 Markdown 内容，这里会实时显示预览</Text>
            )}
          </Card>
        </Flex>
      </Flex>
    </Loading>
  );
};

export default AnnouncementDetails;
