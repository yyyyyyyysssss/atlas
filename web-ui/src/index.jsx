import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import reportWebVitals from './reportWebVitals';
import App from './App';
import { Provider } from 'react-redux';
import { message } from 'antd';
import store from './redux/store';
import './i18n'
import '@rainbow-me/rainbowkit/styles.css';
import { getDefaultConfig, RainbowKitProvider } from '@rainbow-me/rainbowkit';
import { WagmiProvider, http } from 'wagmi';
import {
  mainnet,
  polygon,
  optimism,
  arbitrum,
  base,
  sepolia
} from 'wagmi/chains';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const config = getDefaultConfig({
  appName: 'Atlas',
  projectId: '9bc01ae3cf92e786ed8a61148e15a7ee',
  chains: [
    mainnet,
    polygon,
    optimism,
    arbitrum,
    base,
  ],
  // 显式指定传输节点，绕过默认配置中可能存在 CORS 限制的节点
  transports: {
    [mainnet.id]: http('https://cloudflare-eth.com'),
    [polygon.id]: http('https://polygon-rpc.com'),
    [optimism.id]: http('https://mainnet.optimism.io'),
    [arbitrum.id]: http('https://arb1.arbitrum.io/rpc'),
    [base.id]: http('https://mainnet.base.org'),
  },
  ssr: false,
})

const queryClient = new QueryClient()

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <WagmiProvider config={config}>
    <QueryClientProvider client={queryClient}>
      <RainbowKitProvider>
        <Provider store={store}>
          <App />
        </Provider>
      </RainbowKitProvider>
    </QueryClientProvider>
  </WagmiProvider>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();

message.config({
  top: 60,
  duration: 3,
  maxCount: 5,
});
