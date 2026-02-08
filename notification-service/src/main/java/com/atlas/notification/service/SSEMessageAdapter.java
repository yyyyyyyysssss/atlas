package com.atlas.notification.service;


import com.atlas.common.api.enums.ChannelType;
import com.atlas.notification.domain.mode.MessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/1/30 15:30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SSEMessageAdapter implements MessageAdapter{

    @Override
    public boolean support(ChannelType channelType) {
        return channelType == ChannelType.SSE;
    }

    // todo 待实现
    @Override
    public void send(MessagePayload payload, List<String> targets) {

    }
}
