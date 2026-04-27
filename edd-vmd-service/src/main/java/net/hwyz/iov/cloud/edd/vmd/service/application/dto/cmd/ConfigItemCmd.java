package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置项 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigItemCmd {

    private Long id;
    private String family;
    private String code;
    private String name;
    private String type;
    private String unit;
    private Boolean capability;
    private Boolean display;
    private Boolean cache;
    private String description;

}
