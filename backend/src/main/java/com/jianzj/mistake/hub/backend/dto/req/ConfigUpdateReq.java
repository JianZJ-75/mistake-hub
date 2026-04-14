package com.jianzj.mistake.hub.backend.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <p>
 * 修改系统配置 Req
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Data
public class ConfigUpdateReq {

    /** 配置键 */
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    /** 配置值 */
    @NotBlank(message = "配置值不能为空")
    private String configValue;
}
