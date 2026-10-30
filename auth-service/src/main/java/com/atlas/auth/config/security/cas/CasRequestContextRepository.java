package com.atlas.auth.config.security.cas;

public interface CasRequestContextRepository {

    /**
     * 保存 CAS 请求上下文
     *
     * @return 上下文 ID
     */
    String save(CasRequestContext context);

    /**
     * 获取并消费 CAS 请求上下文
     *
     * @param contextId 上下文 ID
     * @return 请求上下文，不存在或已消费返回 null
     */
    CasRequestContext consume(String contextId);

}
