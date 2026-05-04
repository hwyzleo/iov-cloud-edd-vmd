package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildConfigDto {

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
    private String description;

}