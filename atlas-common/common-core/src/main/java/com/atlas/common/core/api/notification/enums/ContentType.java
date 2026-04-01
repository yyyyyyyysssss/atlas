package com.atlas.common.core.api.notification.enums;

import com.atlas.common.core.api.notification.exception.NotificationException;

public enum ContentType {

    TEXT,

    JSON,

    HTML,
    ;

    public static ContentType getContentType(ChannelType channelType){
        switch (channelType){
            case EMAIL :
                return ContentType.HTML;
            case SMS:
                return ContentType.TEXT;
            case INBOX :
                return ContentType.JSON;
            default:
                throw new NotificationException("未知的渠道");
        }
    }
}
