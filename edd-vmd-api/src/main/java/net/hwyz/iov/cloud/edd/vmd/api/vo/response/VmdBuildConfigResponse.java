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

    private String seriesCode;

    private String modelCode;

    private String baseModelCode;

    private String code;

    private String name;

    private String nameEn;

    private String vehicleStageCode;

    private Boolean enable;

    private Integer sort;

    private List<VmdBuildConfigFeatureCodeResponse> featureCodes;

}