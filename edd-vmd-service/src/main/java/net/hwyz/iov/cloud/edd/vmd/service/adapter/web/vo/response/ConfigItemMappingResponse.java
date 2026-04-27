package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台配置项映射
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ConfigItemMappingResponse {

    /**
     * 主键
     */
    private Long id;

    /**
     * 配置项代码
     */
    private String configItemCode;

    /**
     * 源系统
     */
    private String sourceSystem;

    /**
     * 源系统代码
     */
    private String sourceCode;

    /**
     * 源系统值
     */
    private String sourceValue;

    /**
     * 映射的枚举值编码
     */
    private String targetOptionCode;

    /**
     * 映射值
     */
    private String targetValue;

    /**
     * 创建时间
     */
    private Date createTime;

}
