package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdBuildConfigResponse {

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

    /**
     * 选项值列表（CR-018重命名，原featureCodes）
     */
    private List<VmdBuildConfigOptionCodeResponse> optionCodes;

}
