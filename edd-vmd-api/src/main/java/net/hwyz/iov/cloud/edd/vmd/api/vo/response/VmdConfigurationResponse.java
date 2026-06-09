package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdConfigurationResponse {

    private Long id;

    private String platformCode;

    private String carLineCode;

    private String modelCode;

    private String baseModelCode;

    private String brandCode;

    private String code;

    private String name;

    private String nameEn;

    private String vehicleStageCode;

    private Boolean enable;

    private Integer sort;

    private List<VmdConfigurationFeatureCodeResponse> featureCodes;

    /**
     * 选项值列表(原featureCodes, CR-018重命名)
     */
    private List<VmdConfigurationFeatureCodeResponse> optionCodes;

}
