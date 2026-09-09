package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台配置 响应（CR-047：基础字段 + 产品树派生字段，旧层级冗余字段已下线）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationResponse {

    private Long id;

    private String code;

    private String name;

    private String nameLocal;

    private String variantCode;

    /**
     * 派生：车型代码（沿产品树补全，CR-047）
     */
    private String modelCode;

    /**
     * 派生：车系代码（沿产品树补全，CR-047）
     */
    private String carLineCode;

    /**
     * 派生：平台代码（沿产品树补全，CR-047）
     */
    private String platformCode;

    /**
     * 派生：品牌代码（沿产品树补全，CR-047）
     */
    private String brandCode;

    private Date createTime;

}
