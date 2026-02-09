package com.atlas.notification.adapter;


import com.atlas.common.api.enums.ChannelType;
import com.atlas.notification.domain.mode.MessagePayload;

import java.util.List;

public interface MessageAdapter {

    /**
     * 判断当前适配器是否支持该渠道
     * @param channelType 渠道枚举 (EMAIL, SSE, SMS等)
     * @return boolean
     */
    boolean support(ChannelType channelType);

    /**
     * 执行消息发送
     * @param payload 已经渲染好的消息载体 (TextPayload, HtmlPayload等)
     * @param targets 接收者列表 (用户ID、邮箱地址或Token)
     */
    void send(MessagePayload payload, List<String> targets);

}
