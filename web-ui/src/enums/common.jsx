

export const AuthorityType = Object.freeze({
    MENU: 'MENU',
    BUTTON: 'BUTTON',
    API: 'API'
})


export const RequestMethod = Object.freeze({
    GET: 'GET',
    POST: 'POST',
    PUT: 'PUT',
    PATCH: 'PATCH',
    DELETE: 'DELETE',
    ALL: '*'
})

export const OperationMode = Object.freeze({
    ADD: { value: 'ADD', description: '新增' },
    EDIT: { value: 'EDIT', description: '编辑' },
    VIEW: { value: 'VIEW', description: '查看' }
})