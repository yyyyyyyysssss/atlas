package com.atlas.security.model;

import com.atlas.common.core.validation.ValidApiUrls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 10:01
 */
@Getter
@Setter
public class AuthorityUrl {

    @NotBlank(message = "请求方法不能为空")
    @Pattern(regexp = "^(?i)(GET|POST|PUT|PATCH|DELETE|\\*)$", message = "请求方法仅支持 GET、POST、PUT、PATCH、DELETE 或 *")
    private String method;

    @NotBlank(message = "路径不能为空")
    @ValidApiUrls(message = "路径不合法")
    private String url;

}
