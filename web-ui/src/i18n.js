import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import en from './locale/en.json'
import zh from './locale/zh.json'


i18n
    .use(LanguageDetector) // 自动检测语言
    .use(initReactI18next) // 绑定 react-i18next
    .init({
        fallbackLng: 'zh',  // 默认中文
        debug: false,  // 开启调试模式
        interpolation: {
            escapeValue: false, // React 默认已经做了转义
        },
        resources: {
            en: {
                translation: en
            },
            zh: {
                translation: zh
            }
        }
    })