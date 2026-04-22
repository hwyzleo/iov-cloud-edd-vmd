package net.hwyz.iov.cloud.edd.vmd.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

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
@EqualsAndHashCode(callSuper = true)
public class ConfigItemOptionVo extends BaseRequest {

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
