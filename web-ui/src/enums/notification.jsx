


export const AnnouncementStatus = Object.freeze({
    DRAFT: { value: 'DRAFT', label: '草稿', color: 'gray' },      // 默认灰色，代表未生效
    PUBLISHED: { value: 'PUBLISHED', label: '已发布', color: 'success' }, // 成功绿，代表在线
})

export const AnnouncementType = Object.freeze({
    URGENT: { value: 'URGENT', label: '紧急', color: 'error' },     // 红色，最高优先级
    RELEASE: { value: 'RELEASE', label: '发版', color: 'processing' }, // 品牌蓝，业务更新
    NOTICE: { value: 'NOTICE', label: '通知', color: 'cyan' },      // 青色，日常告知
    MAINTAIN: { value: 'MAINTAIN', label: '维护', color: 'purple' }  // 紫色/深蓝，技术维护
})