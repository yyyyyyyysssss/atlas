package com.atlas.auth.config.security.cas;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/6 11:07
 */
public interface CasProxyGrantingTicketRepository {

    void save(CasProxyGrantingTicketContext context);

    CasProxyGrantingTicketContext findByTicket(String ticket);

    void deleteByTicket(String ticket);

    boolean exists(String ticket);

}
