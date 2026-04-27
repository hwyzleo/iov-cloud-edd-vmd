package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 特征族 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFamilyCmd {

    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String type;
    private Boolean mandatory;
    private Boolean enable;
    private Integer sort;
    private String description;

}
