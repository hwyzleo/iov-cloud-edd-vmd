package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationDto {

    private Long id;
    private String code;
    private String name;
    private String nameLocal;
    private String variantCode;
    private String description;

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

}
