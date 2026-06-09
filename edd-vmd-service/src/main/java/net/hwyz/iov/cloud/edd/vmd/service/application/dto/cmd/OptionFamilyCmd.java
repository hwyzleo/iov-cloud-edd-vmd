package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 选装族命令 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionFamilyCmd {

    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String type;
    private Boolean mandatory;
    private Boolean enable;
    private Integer sort;

}
