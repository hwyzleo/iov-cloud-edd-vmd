package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 特征值 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureCodeCmd {

    private Long id;
    private String familyCode;
    private String code;
    private String name;
    private String nameEn;
    private String val;
    private Boolean enable;
    private Integer sort;
    private String description;

}
