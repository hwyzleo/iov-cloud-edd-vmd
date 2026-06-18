package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车载节点 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleNodeDto {

    private Long id;
    private String code;
    private String name;
    private String nameLocal;
    private String nodeType;
    private String deviceCategory;
    private String funcDomain;
    private String otaSupport;
    private Boolean core;
    private Integer sort;

}
