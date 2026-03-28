import React, { createContext, useEffect, useMemo, useState } from 'react';
import './App.css';
import router from './router/router';
import { RouterProvider } from 'react-router-dom';
import { ConfigProvider, message } from 'antd';
import { useSelector } from 'react-redux';
import 'antd/dist/reset.css';
import zhCN from 'antd/locale/zh_CN';
import enUS from 'antd/locale/en_US';
import 'dayjs/locale/zh-cn';
import NProgress from 'nprogress';
import { setMessageApi } from './utils/MessageUtil.jsx';
import { AuthProvider } from './router/AuthProvider.jsx';
import tinycolor from 'tinycolor2';
import dayjs from 'dayjs'
import NoDataEmpty from './components/NoDataEmpty.js';
import Cookies from 'js-cookie'
import { useSse } from './hooks/useSse.jsx';

dayjs.locale('zh-cn')

NProgress.configure({
  showSpinner: false
})

const App = () => {

  const [api, contextHolder] = message.useMessage()

  const colorPrimary = useSelector(state => state.layout.colorPrimary)

  const language = useSelector(state => state.layout.language)

  const themeValue = useSelector(state => state.layout.theme)


  const accessToken = Cookies.get("accessToken")
  const sseUrl = `/api/notification/v1/notification/sse/subscribe?terminal=web&access_token=${accessToken}`

  const { status } = useSse(sseUrl, {
    // 2. 处理业务消息 (对应后端的 message_event)
    onMessage: (data) => {
      console.log('message:', data);
    },

    // 3. 连接成功后的逻辑
    onConnected: () => {
      console.log('listener...');
    }
  });

  useEffect(() => {
    document.documentElement.style.setProperty('--color-primary', colorPrimary)
  }, [colorPrimary])

  const activeBgColor = useMemo(() => tinycolor(colorPrimary)
    .lighten(20)
    .setAlpha(0.2)
    .toString(), [colorPrimary]
  )

  const hoverAndActiveBgColor = useMemo(() =>
    tinycolor(activeBgColor)
      .darken(10)   // 将激活状态颜色稍微加深
      .setAlpha(0.3) // 增加透明度
      .toString(),
    [activeBgColor]
  );


  const themeConfig = useMemo(() => ({
    dark: {
      cssVar: true,
      token: {
        colorText: 'rgba(255, 255, 255, 0.88)', // 白色文本
        colorTextSecondary: '255, 255, 255, 0.6', //第二梯度的文本色
        colorTextTertiary: 'rgba(255, 255, 255, 0.2)', //第三梯度的文本色
        colorTextQuaternary: 'rgba(255, 255, 255, 0.18)', // 第四梯度的文本色
        colorIcon: 'rgba(255, 255, 255, 0.55)', // 图标
        colorIconHover: 'rgba(255, 255, 255, 0.75)', // 图标悬浮
        colorSplit: 'rgba(255, 255, 255, 0.06)', // 较浅的分割线颜色
        colorTextDescription: 'rgba(255, 255, 255, 0.45)', // 较浅的文本描述颜色
        colorTextDisabled: 'rgba(255, 255, 255, 0.30)', //控制禁用状态下的字体颜色
        colorPrimary: colorPrimary, // 保持主色一致
        colorLink: colorPrimary, // 保持链接颜色一致
        borderRadius: 6, // 圆角
        fontSize: 14, // 字体大小
        colorBgContainer: '#252525', // 容器背景色：深色背景
        colorBgElevated: '#252525', //浮层容器背景色
        colorFillAlter: 'rgba(255,255,255,0.02)',
        controlItemBgHover: '#3A3A3A', //控制组件项在鼠标悬浮时的背景颜色。
        controlItemBgActive: activeBgColor, // 控制组件项在激活状态下的背景颜色
        controlItemBgActiveHover: hoverAndActiveBgColor, // 控制组件项在鼠标悬浮且激活状态下的背景颜色
        colorBgContainerDisabled: 'rgba(255, 255, 255, 0.08)', //容器在禁用状态下的背景色
        colorBorder: 'rgba(255, 255, 255, 0.2)', //边框颜色 
        colorBorderSecondary: 'rgba(255, 255, 255, 0.06)', //辅助性的边框颜色
        colorTextPlaceholder: 'rgba(255, 255, 255, 0.18)', // 提示文字
        controlOutline: 'none', //激活时的轮廓颜色
        boxShadow: '0 6px 16px 0 rgba(0, 0, 0, 0.2), 0 3px 6px -4px rgba(0, 0, 0, 0.25), 0 9px 28px 8px rgba(0, 0, 0, 0.15)',
        boxShadowSecondary: '0 6px 16px 0 rgba(0, 0, 0, 0.2), 0 3px 6px -4px rgba(0, 0, 0, 0.25), 0 9px 28px 8px rgba(0, 0, 0, 0.15)',
        boxShadowTertiary: '0 1px 2px 0 rgba(0, 0, 0, 0.05), 0 1px 4px -1px rgba(0, 0, 0, 0.01), 0 2px 3px 0 rgba(0, 0, 0, 0.1)',
      },
      components: {
        Breadcrumb: {
          linkHoverColor: colorPrimary, // 面包屑链接悬浮颜色
        },
        Table: {
          headerBg: '#333333', // 表格头部背景色：更暗的背景
          rowHoverBg: '#333333', // 表格行悬浮背景色：深灰色
          bodySortBg: '#333333', // 表格排序背景色
          headerSortActiveBg: '#444444', // 排序头部激活背景色
          headerSortHoverBg: '#444444', // 排序头部悬浮背景色
          fixedHeaderSortActiveBg: '#444444', // 固定表头排序激活背景色
        },
        Layout: {
          // 顶部背景色
          headerBg: '#1c1c1c', // 更深的顶部背景色
          // 侧边栏背景色
          siderBg: '#181818', // 侧边栏背景色
          // 主体部分背景色
          bodyBg: '#121212', // 主体背景色：深色背景
          // 顶部高度
          headerHeight: 64,
          // 顶部内边距
          headerPadding: 0,
        },
        Menu: {
          // 菜单项背景
          darkItemBg: '#181818',
          // 菜单项悬浮背景
          darkItemHoverBg: '#3A3A3A',
          // 菜单项背景色
          darkSubMenuItemBg: '#1f1f1f', // 菜单项背景色，深灰色
          darkItemSelectedBg: activeBgColor,
          darkPopupBg: '#181818'
        },
        Dropdown: {

        },
        Tree: {

        },
        TreeSelect: {

        },
        Tabs: {
          cardBg: '#1a1a1a'
        },
        Button: {
          primaryShadow: '0 2px 0 rgba(5,145,255,0.1)',
          dangerShadow: '0 2px 0 rgba(0, 0, 0, 0.2)',
        },
        Input: {

        },
        Select: {
          colorBgBase: '#1C1C1C',
        },
        Splitter: {

        },
        Segmented: {
          itemHoverColor: 'rgba(0,0,0,0.88)',
          itemColor: 'rgba(0,0,0,0.65)'
        }
      },
    },
    light: {
      cssVar: true,
      token: {
        colorText: 'rgba(0,0,0,0.88)',
        colorSplit: 'rgba(5,5,5,0.06)',
        colorTextDescription: 'rgba(0,0,0,0.45)',
        colorPrimary: colorPrimary,
        colorLink: colorPrimary,
        borderRadius: 6,
        colorBgContainer: 'white',
        fontSize: 14,
        colorBorder: '#d9d9d9',
        colorBorderSecondary: '#f0f0f0',
      },
      components: {
        Breadcrumb: {
          linkHoverColor: colorPrimary
        },
        Table: {
          headerBg: '#fafafa',
          rowHoverBg: '#fafafa',
          bodySortBg: '#fafafa',
          headerSortActiveBg: '#fafafa',
          headerSortHoverBg: '#fafafa',
          fixedHeaderSortActiveBg: '#fafafa',
          footerBg: '#fafafa'
        },
        Layout: {
          //顶部背景色
          headerBg: '#fafafa',
          //侧边栏背景色
          siderBg: 'white',
          //主体部分背景色
          bodyBg: '#f5f5f5',
          //顶部高度
          headerHeight: 64,
          //顶部内边距
          headerPadding: 0
        },
        Tabs: {
          cardBg: 'white'
        },
        Menu: {

        }
      }
    }
  }), [colorPrimary, activeBgColor])

  setMessageApi(api)

  return (
    <ConfigProvider
      locale={language === 'zh' ? zhCN : enUS}
      theme={themeConfig[themeValue]}
      renderEmpty={() => (<NoDataEmpty />)}
    >
      <AuthProvider>
        {contextHolder}
        <RouterProvider
          future={{
            v7_startTransition: true,
            v7_relativeSplatPath: true
          }}
          router={router}
        />
      </AuthProvider>
    </ConfigProvider>
  );
}

export default App;
