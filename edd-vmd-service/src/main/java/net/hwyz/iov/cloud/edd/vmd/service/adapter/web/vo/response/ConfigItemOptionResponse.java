package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台配置项枚举值
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ConfigItemOptionResponse {

    /**
     * 主键
     */
    private Long id;

    /**
     * 配置项编码
     */
    private String configItemCode;

    /**
     * 枚举值编码
     */
    private String code;

    /**
     * 枚举值名称
     */
    private String name;

    /**
     * 创建时间
     */
    private Date createTime;

}
