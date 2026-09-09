package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.*;

import java.util.List;

/**
 * 配置响应（CR-047：Configuration 最小投影基础字段 + 产品树派生字段）
 * <p>
 * 基础字段：code / name / nameLocal / variantCode / description；
 * 派生字段：modelCode / carLineCode / platformCode / brandCode 沿
 * Configuration → Variant → Model → CarLine / Platform → Brand 补全，
 * 上层投影缺失时相应字段为 null（LEFT JOIN 降级，不阻断基础查询）。
 * </p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdConfigurationResponse {

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

    /**
     * 选项值列表（CR-018重命名，原featureCodes）
     */
    private List<VmdConfigurationOptionCodeResponse> optionCodes;

}
