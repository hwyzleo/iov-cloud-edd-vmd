package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationCmd {

    private Long id;
    private String platformCode;
    private String carLineCode;
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
