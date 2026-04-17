import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { Card, Flex, Button, Typography, Timeline, theme, Badge, Tooltip, Modal, Form, Input, DatePicker, TimePicker, Tag, Radio, Segmented, Alert, App } from 'antd';
import {
    LeftOutlined,
    RightOutlined,
    PlusOutlined,
    CalendarOutlined,
    CheckCircleOutlined,
    EditOutlined,
    FireOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { Plus } from 'lucide-react';
import { useRequest } from 'ahooks';
import { createUserWorkSchedule, getUserWorkSchedule, updateUserWorkSchedule } from '../../services/UserProfileService';
import Loading from '../../components/loading';
import { useDispatch } from 'react-redux';
import { setTodayTaskCount } from '../../redux/slices/userSlice';

const { Text, Title } = Typography;

const priorityMap = {
    1: {
        color: 'error',
        label: '紧急',
        icon: <FireOutlined style={{ fontSize: 10 }} />
    },
    2: {
        color: 'orange',
        label: '重要',
        icon: null // 重要事项不需要图标
    },
    // 3 即使配置了也不会被渲染，保持结构一致即可
    3: {
        color: 'default',
        label: '普通',
        icon: null
    },
};

const WeeklyScheduleCard = () => {
    const { token } = theme.useToken()

    const { message } = App.useApp()

    const [form] = Form.useForm()

    const dispatch = useDispatch()

    const [data, setData] = useState([])

    const [selectedDate, setSelectedDate] = useState(dayjs()) // 当前选中的日期
    const [baseDate, setBaseDate] = useState(dayjs().startOf('week')) // 当前周视图的起点（周日或周一）

    const [isModalOpen, setIsModalOpen] = useState(false)

    const [isPast, setIsPast] = useState(false)

    const { runAsync: getDataAsync, loading: getDataLoading } = useRequest(getUserWorkSchedule, {
        manual: true
    })

    const { runAsync: createAsync, loading: createLoading } = useRequest(createUserWorkSchedule, {
        manual: true
    })

    const { runAsync: updateAsync, loading: updateLoading } = useRequest(updateUserWorkSchedule, {
        manual: true
    })

    const fetchData = useCallback(async (start, end) => {
        const result = await getDataAsync(start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD'))
        setData(result || [])
        // 计算今日合计并存入 Redux
        const today = dayjs()
        if (!today.isBefore(start, 'day') && !today.isAfter(end, 'day')) {
            const todayStr = today.format('YYYY-MM-DD');
            const count = (result || [])
                .filter(t =>
                    dayjs(t.startTime).format('YYYY-MM-DD') === todayStr
                )
                .length
            dispatch(setTodayTaskCount({ todayTaskCount: count }))
        }
    }, [getDataAsync])

    useEffect(() => {
        const startDate = baseDate
        const endDate = baseDate.add(6, 'day')
        fetchData(startDate, endDate);
    }, [baseDate])

    const taskDates = useMemo(() => {
        return new Set(data.map(t => dayjs(t.startTime).format('YYYY-MM-DD')));
    }, [data])

    // 2. 计算当前视图显示的 7 天数据
    const days = useMemo(() => {
        return Array.from({ length: 7 }).map((_, i) => baseDate.add(i, 'day'));
    }, [baseDate]);

    const currentTasks = useMemo(() => {
        const selectedStr = selectedDate.format('YYYY-MM-DD')
        return data.filter(task =>
            dayjs(task.startTime).format('YYYY-MM-DD') === selectedStr
        );
    }, [data, selectedDate])

    // 3. 导航逻辑
    const nextWeek = () => setBaseDate(prev => prev.add(7, 'day'));
    const prevWeek = () => setBaseDate(prev => prev.subtract(7, 'day'));
    const backToToday = () => {
        const today = dayjs()
        setBaseDate(today.startOf('week'))
        handleSelectDate(today)
    }

    const handleSelectDate = (date) => {
        setSelectedDate(date)
    }

    const showModal = (record = null) => {
        setIsModalOpen(true)
        form.resetFields()
        if (record) {
            const startTime = dayjs(record.startTime)
            const pastStatus = startTime.isBefore(dayjs().startOf('day'))
            setIsPast(pastStatus)
            form.setFieldsValue({
                ...record,
                date: startTime,
                time: startTime
            })
        } else {
            setIsPast(false)
            const now = dayjs()
            const minutes = now.minute()
            let nextTime
            if (minutes < 30) {
                nextTime = now.minute(30).second(0) // 设为本小时 30 分
            } else {
                nextTime = now.add(1, 'hour').startOf('hour') // 设为下个点
            }
            form.setFieldsValue({
                date: selectedDate,
                time: nextTime
            })
        }

    }

    const isDatePast = selectedDate.isBefore(dayjs(), 'day')

    const handleSave = async (continueAdding = false) => {
        const values = await form.validateFields()
        // 合并 DatePicker 和 TimePicker 的值
        const combinedStartTime = dayjs(values.date)
            .hour(values.time.hour())
            .minute(values.time.minute())
            .second(0)
            .format('YYYY-MM-DD HH:mm:ss')
        const payload = {
            ...values,
            startTime: combinedStartTime,
        }
        if (payload.id) {
            await updateAsync(payload)
        } else {
            await createAsync(payload)
        }



        const newTaskDate = dayjs(values.date);
        // 如果新日期超出了当前 baseDate 的那一周范围
        const weekEnd = baseDate.add(6, 'day')
        if (newTaskDate.isBefore(baseDate, 'day') || newTaskDate.isAfter(weekEnd, 'day')) {
            // 更新 baseDate 会自动触发 useEffect，从而请求新数据
            setBaseDate(newTaskDate.startOf('week'))
            setSelectedDate(newTaskDate);
        } else {
            // 还在本周，手动刷一遍 fetchData
            fetchData(baseDate, baseDate.add(6, 'day'))
        }

        if (continueAdding) {
            // 保存并继续
            const nextTime = dayjs(values.time).add(30, 'minute')
            // 重置表单，但保留日期和递增后的时间
            form.setFieldsValue({
                ...form.getFieldsValue(), // 保留部分字段（如优先级）
                title: '',                // 清空标题
                content: '',              // 清空描述
                time: nextTime,           // 时间递增 30 分钟
            })
            message.success('已保存，请继续添加')
        } else {
            setIsModalOpen(false)
            message.success('保存成功')
        }

    }

    const disabledDate = (current) => {
        // current < 今天 0 点
        return current && current < dayjs().startOf('day')
    }


    return (
        <>
            <Card
                title={
                    <Flex align="center" gap={8}>
                        <CalendarOutlined style={{ color: token.colorPrimary }} />
                        <span>工作日程</span>
                        <Text type="secondary" style={{ fontWeight: 'normal', marginLeft: 8 }}>
                            {baseDate.format('YYYY年MM月')}
                        </Text>
                    </Flex>
                }
                extra={
                    <Flex gap={8} align="center">
                        <Button type='link' size="small" onClick={backToToday}>回到今日</Button>
                    </Flex>
                }
                variant="borderless"
            >
                <Flex vertical gap={24}>
                    <Flex justify="space-between" align="center">
                        <Button
                            type="text"
                            icon={<LeftOutlined />}
                            onClick={prevWeek}
                            style={{ color: token.colorTextDescription }}
                        />
                        <Flex flex={1} justify="space-around">
                            {days.map((date) => {
                                const dateStr = date.format('YYYY-MM-DD')
                                const isSelected = date.isSame(selectedDate, 'day')
                                const isToday = date.isSame(dayjs(), 'day')
                                const hasTasks = taskDates.has(dateStr)
                                return (
                                    <Flex
                                        key={date.format('YYYY-MM-DD')}
                                        vertical
                                        align="center"
                                        justify="center"
                                        gap={10}
                                    >
                                        <Text
                                            style={{
                                                fontSize: 12,
                                                opacity: 0.8
                                            }}
                                        >
                                            {date.format('dd')}
                                        </Text>
                                        <Flex
                                            onClick={() => handleSelectDate(date)}
                                            style={{
                                                cursor: 'pointer',
                                                width: 38,
                                                height: 38,
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                borderRadius: '50%',
                                                transition: 'all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1)',
                                                background: isSelected ? token.colorPrimary : 'transparent',
                                            }}
                                            vertical
                                        >
                                            <Text
                                                style={{
                                                    fontWeight: 'bold',
                                                }}
                                            >
                                                {date.date()}
                                            </Text>

                                        </Flex>
                                        <Text style={{
                                            width: 4,
                                            height: 4,
                                            borderRadius: '50%',
                                            background: hasTasks ? token.colorPrimary : 'transparent',
                                            marginTop: 4
                                        }} />
                                    </Flex>
                                );
                            })}
                        </Flex>
                        <Button
                            type="text"
                            icon={<RightOutlined />}
                            onClick={nextWeek}
                            style={{ color: token.colorTextDescription }}
                        />
                    </Flex>

                    {/* --- 下方待办详情 --- */}
                    <Flex
                        gap={10}
                        vertical
                    >
                        <Flex align='center' justify='space-between'>
                            <Flex align="baseline" gap={8} >
                                <Title level={3} style={{ margin: 0 }}>{selectedDate.date()}</Title>
                                <Text strong>{selectedDate.format('dddd')}</Text>
                                {currentTasks.length > 0 && (
                                    <Text type="secondary" style={{ fontSize: 12 }}>
                                        {currentTasks.length} 条事项
                                    </Text>
                                )}
                            </Flex>
                            {!isDatePast && (
                                <Tooltip title="新增待办">
                                    <Button onClick={() => showModal(null)} type="link" size="small" icon={<PlusOutlined />} />
                                </Tooltip>
                            )}
                        </Flex>
                        <Loading spinning={getDataLoading}>
                            <Flex
                                style={{
                                    width: '100%',
                                    height: '200px',
                                    overflowY: 'auto',
                                    overflowX: 'hidden',
                                    paddingTop: '10px',
                                    paddingRight: '20px'
                                }}
                                vertical
                            >
                                {currentTasks.length > 0 ? (
                                    <Timeline
                                        items={currentTasks.map(item => ({
                                            children: (
                                                <Flex
                                                    vertical
                                                >
                                                    <Text style={{ color: token.colorPrimary }}>
                                                        {dayjs(item.startTime).format('HH:mm')}
                                                    </Text>
                                                    <Flex justify="space-between" align="flex-start">
                                                        <Flex align="center" gap={8}>
                                                            <Text strong>{item.title}</Text>
                                                            {item.priority && item.priority !== 3 && (
                                                                <Tag
                                                                    color={priorityMap[item.priority].color}
                                                                    bordered={false}
                                                                    style={{
                                                                        margin: 0,
                                                                        fontSize: '10px',
                                                                        lineHeight: '16px',
                                                                        paddingInline: '6px'
                                                                    }}
                                                                >
                                                                    <Flex align="center" gap={2}>
                                                                        {item.priority === 1 && priorityMap[item.priority].icon}
                                                                        {priorityMap[item.priority].label}
                                                                    </Flex>
                                                                </Tag>
                                                            )}
                                                        </Flex>
                                                        <Button
                                                            type="text"
                                                            size="small"
                                                            icon={<EditOutlined style={{ fontSize: 12, color: token.colorPrimary }} />}
                                                            onClick={(e) => {
                                                                e.stopPropagation()
                                                                showModal(item)
                                                            }}
                                                        />
                                                    </Flex>
                                                    <Text type='secondary'>{item.content}</Text>
                                                </Flex>
                                            )
                                        }))}
                                    />

                                ) : (
                                    <Flex
                                        gap={16}
                                        justify="center"
                                        align="center"
                                        style={{ height: 200 }}
                                        vertical
                                    >
                                        <Flex justify="center" align="center" >
                                            <Text type="secondary">这一天很清闲，没有任何待办事项</Text>
                                        </Flex>
                                        {!isDatePast && (
                                            <Button
                                                block
                                                type="dashed"
                                                onClick={() => showModal(null)}
                                                style={{ height: '60px', width: '20%', borderRadius: token.borderRadiusLG }}
                                            >
                                                <Flex vertical align="center" justify="center" gap={4}>
                                                    <Text type="secondary"><Plus size={16} /></Text>
                                                    <Text type="secondary" style={{ fontSize: '12px' }}>添加待办事项</Text>
                                                </Flex>
                                            </Button>
                                        )}
                                    </Flex>
                                )}
                            </Flex>
                        </Loading>
                    </Flex>
                </Flex>
            </Card>
            <Modal
                title="工作事项"
                open={isModalOpen}
                centered
                width={400}
                onOk={handleSave}
                okButtonProps={{
                    loading: createLoading || updateLoading
                }}
                onCancel={() => setIsModalOpen(false)}
                afterClose={() => form.resetFields()}
                footer={
                    [
                        <Button key="cancel" onClick={() => setIsModalOpen(false)}>
                            取消
                        </Button>,
                        // 只有在“新增”模式下才显示“保存并继续”
                        !form.getFieldValue('id') && (
                            <Button
                                key="continue"
                                type="default"
                                loading={createLoading}
                                onClick={() => handleSave(true)}
                            >
                                保存并继续
                            </Button>
                        ),
                        <Button
                            key="submit"
                            type="primary"
                            loading={createLoading || updateLoading}
                            onClick={() => handleSave(false)}
                        >
                            确定
                        </Button>,
                    ]
                }
                destroyOnHidden
            >
                <Form
                    form={form}
                    labelCol={{ span: 6 }}
                    wrapperCol={{ span: 18 }}
                    layout="horizontal"
                >
                    <Form.Item name="id" hidden >
                        <Input />
                    </Form.Item>
                    <Form.Item
                        name="title"
                        label="事项名称"
                        rules={[{ required: true, message: '请输入名称' }]}
                        extra={isPast && (
                            <Text type="warning" style={{ fontSize: '12px' }}>
                                <InfoCircleOutlined style={{ marginRight: 4 }} />
                                该事项已过期，基础信息已锁定
                            </Text>
                        )}
                    >
                        <Input disabled={isPast} placeholder="要做什么？" />
                    </Form.Item>

                    <Form.Item name="priority" label="优先级">
                        <Segmented
                            block
                            disabled={isPast}
                            options={[
                                { label: '普通', value: 3 },
                                { label: '重要', value: 2 },
                                { label: '紧急', value: 1 },
                            ]}
                        />
                    </Form.Item>

                    <Form.Item name="date" label="日期" style={{ flex: 1 }} rules={[{ required: true, message: '请选择日期' }]}>
                        <DatePicker disabled={isPast} disabledDate={disabledDate} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="time" label="时间" style={{ flex: 1 }} rules={[{ required: true, message: '请选择时间' }]}>
                        <TimePicker disabled={isPast} hourStep={1} minuteStep={5} format="HH:mm" style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="content" label="描述">
                        <Input.TextArea rows={3} placeholder="添加点描述信息..." />
                    </Form.Item>
                </Form>
            </Modal>
        </>
    );
};

export default WeeklyScheduleCard;