package com.atlas.auth.event;


import com.atlas.auth.enums.ClientProtocol;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientAuthorizedEvent extends ApplicationEvent {

    private final Long userId;

    private final String clientId;

    private final ClientProtocol protocol;

    public ClientAuthorizedEvent(Object source, Long userId, String clientId, ClientProtocol protocol) {
        super(source);
        this.userId = userId;
        this.clientId = clientId;
        this.protocol = protocol;
    }
}
