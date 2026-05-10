package com.atlas.auth.domain.vo;


import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrAuthTicketVO {

    private String sceneId;

    private String qrUrl;

    private Long expireSeconds;

}
