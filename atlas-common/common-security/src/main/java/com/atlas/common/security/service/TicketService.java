package com.atlas.common.security.service;

import java.time.Duration;

public interface TicketService {

    /**
     * 创建ticket
     */
    String create(Object value, Duration timeout);


    /**
     * 获取ticket，不删除
     */
    <T> T get(String ticket, Class<T> type);



    /**
     * 消费ticket，获取后删除
     */
    <T> T consume(String ticket, Class<T> type);

}
