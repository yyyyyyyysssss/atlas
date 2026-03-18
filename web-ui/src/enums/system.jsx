

export const OrganizationType = Object.freeze({
    GROUP: { value: 'GROUP', label: '集团' },
    COMPANY: { value: 'COMPANY', label: '公司' },
    DEPT: { value: 'DEPT', label: '部门' },
    TEAM: { value: 'TEAM', label: '团队' },
})

export const OrganizationStatus = Object.freeze({
    ACTIVE: { value: 'ACTIVE', label: '已生效', color: 'success' },
    INACTIVE: { value: 'INACTIVE', label: '已停用', color: 'gray' },
})


export const PositionStatus = Object.freeze({
    ACTIVE: { value: 'ACTIVE', label: '已生效', color: 'success' },
    INACTIVE: { value: 'INACTIVE', label: '已停用', color: 'gray' },
})

export const PositionType = Object.freeze({
    BASE: { value: 'BASE', label: '通用岗位', color: 'processing' },
    ORG: { value: 'ORG', label: '组织岗位', color: 'success' },
})


export const RoleDataScope = Object.freeze({
    ALL: { value: 50, label: '全部', color: 'magenta' },
    CUSTOM: { value: 40, label: '自定义', color: 'orange' },
    COMPANY: { value: 30, label: '本公司', color: 'geekblue' },
    DEPT: { value: 20, label: '本部门', color: 'blue' },
    SELF: { value: 10, label: '本人', color: 'green' },
})