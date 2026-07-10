package com.atlas.user.domain.dto;

import com.atlas.common.core.validation.ValidApiUrls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/10 11:16
 */
@Getter
@Setter
public class AuthorityUrlDTO {

    private Long id;

    private Long authorityId;

    @NotBlank(message = "路径不能为空")
    @ValidApiUrls(message = "路径不合法")
    private String url;

    @NotEmpty(message = "请求方法列表不能为空")
    private List<
            @NotBlank(message = "请求方法不能为空")
            @Pattern(regexp = "^(?i)(GET|POST|PUT|PATCH|DELETE|\\*)$", message = "请求方法仅支持 GET、POST、PUT、PATCH、DELETE 或 *")
            String
            > method;

}
