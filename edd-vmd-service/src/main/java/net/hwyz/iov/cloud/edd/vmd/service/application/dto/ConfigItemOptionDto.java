package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置项枚举值 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigItemOptionDto {

    private Long id;
    private String configItemCode;
    private String code;
    private String name;
    private String description;

}
