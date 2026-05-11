package com.atlas.auth.domain.vo;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrAuthStatusVO {

    private String status;

    private String code;

}
