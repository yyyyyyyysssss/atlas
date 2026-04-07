import React, { useMemo, useState } from 'react';
import { Card, Row, Col, Button, Flex, Typography, Space, theme, Modal, Transfer, List, Tooltip, App } from 'antd';
import { Zap, Plus, ArrowUp, X } from 'lucide-react';
import { useSelector, useDispatch } from 'react-redux';
import { findRouteByPath } from '../../router/router';
import { useNavigate } from 'react-router-dom';
import { useRequest } from 'ahooks';
import { changeWorkbenchShortcuts } from '../../services/UserProfileService';
import { updateShortcuts } from '../../redux/slices/userSlice';

const { Text } = Typography;

const ShortcutCard = () => {

  const { token } = theme.useToken()

  const { message } = App.useApp()

  const { userInfo } = useSelector(state => state.user)

  const flattenMenuItems = useSelector(state => state.layout.flattenMenuItems)

  const [isModalOpen, setIsModalOpen] = useState(false);

  const [targetKeys, setTargetKeys] = useState([])

  const shortcutIds = useMemo(() => userInfo?.settings?.workbench?.shortcuts || [], [userInfo])

  const navigate = useNavigate()

  const dispatch = useDispatch()

  const { runAsync: changeWorkbenchShortcutsAsync, loading: changeWorkbenchShortcutsLoading } = useRequest(changeWorkbenchShortcuts, {
    manual: true
  })

  const userShortcuts = useMemo(() => {
    if (!shortcutIds.length || !flattenMenuItems.length) {
      return []
    }
    // 匹配逻辑
    return shortcutIds
      .map(id => {
        const menu = flattenMenuItems.find(item => String(item.id) === String(id))
        if (!menu) {
          return null
        }
        const route = findRouteByPath(menu.routePath)
        return {
          label: menu.name,
          path: menu.routePath,
          icon: route?.defaultIcon || <Zap size={16} />,
        }
      })
  }, [shortcutIds, flattenMenuItems])

  const transferDataSource = useMemo(() => {
    return flattenMenuItems
      .filter(item => {
        // 排除工作台本身
        const isWorkbench = item.routePath === '/workbench' || item.key === 'workbench';
        const route = findRouteByPath(item.routePath)
        return !isWorkbench && route && route.element
      })
      .map(item => ({
        key: String(item.id),
        title: item.name,
      }))
  }, [flattenMenuItems])

  const handleOpenConfig = () => {
    setTargetKeys(shortcutIds.map(String)) // 同步当前已有的快捷方式
    setIsModalOpen(true)
  }

  const handleOk = async () => {
    await changeWorkbenchShortcutsAsync(targetKeys)
    setIsModalOpen(false)
    dispatch(updateShortcuts(targetKeys))
  }

  const moveUp = (e, index) => {
    e.stopPropagation()
    if (index === 0) return
    const newKeys = [...targetKeys]
    const temp = newKeys[index]
    newKeys[index] = newKeys[index - 1]
    newKeys[index - 1] = temp
    setTargetKeys(newKeys)
  }

  const jumpTo = (path) => {
    navigate(path)
  }

  return (
    <>
      <Card
        title={<Space><Zap size={18} fill={token.colorWarning} stroke={token.colorWarning} /> 快捷开始</Space>}
        extra={<Button type="link" size="small" onClick={handleOpenConfig}>配置</Button>}
        variant="borderless"
      >
        <Row gutter={[16, 16]}>
          {userShortcuts.map((item) => (
            <Col span={6} key={item.label}>
              <Button
                block
                size="large"
                onClick={() => jumpTo(item.path)}
                type="text"
                className="atlas-float-trigger"
                style={{
                  background: token.colorFillAlter,
                  height: '60px',
                  borderRadius: token.borderRadiusLG
                }}
              >
                <Flex vertical align="center" justify="center" gap={4}>
                  {item.icon}
                  <Text size="small" strong>{item.label}</Text>
                </Flex>
              </Button>
            </Col>
          ))}
          {userShortcuts.length < 8 && (
            <Col span={6}>
              <Button
                block
                size="large"
                type="dashed"
                onClick={handleOpenConfig}
                style={{ height: '60px', borderRadius: token.borderRadiusLG }}
              >
                <Flex vertical align="center" justify="center" gap={4}>
                  <Plus size={16} />
                  <Text type="secondary" style={{ fontSize: '12px' }}>添加</Text>
                </Flex>
              </Button>
            </Col>
          )}
        </Row>
      </Card>
      <Modal
        title="配置快捷入口 (最多8个)"
        open={isModalOpen}
        onOk={handleOk}
        onCancel={() => setIsModalOpen(false)}
        okButtonProps={{
          loading: changeWorkbenchShortcutsLoading
        }}
        width={600}
        centered
      >
        <Transfer
          dataSource={transferDataSource}
          showSearch
          listStyle={{
            width: 250,
            height: 300,
          }}
          targetKeys={targetKeys}
          onChange={(nextTargetKeys) => {
            if (nextTargetKeys.length > 8) {
              message.warning('快捷入口最多设置 8 个')
              return
            }
            setTargetKeys(nextTargetKeys)
          }}
          render={item => {
            const index = targetKeys.indexOf(item.key)
            if (index === -1) return item.title

            return (
              <Flex justify="space-between" align="center" style={{ width: '100%', paddingRight: 8 }}>
                <Text size="small">{item.title}</Text>
                {index > 0 && (
                  <Button
                    type="text"
                    size="small"
                    style={{
                      height: 20,
                      padding: '0 4px',
                    }}
                    icon={<ArrowUp size={14} />}
                    onClick={(e) => moveUp(e, index)}
                  />
                )}
              </Flex>
            )
          }}
          titles={['可选菜单', '已添加']}
        />
      </Modal>
    </>
  );
};

export default ShortcutCard;