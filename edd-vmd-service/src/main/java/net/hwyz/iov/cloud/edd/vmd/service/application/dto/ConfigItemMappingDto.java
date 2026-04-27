package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置项映射 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigItemMappingDto {

    private Long id;
    private String configItemCode;
    private String sourceSystem;
    private String sourceCode;
    private String sourceValue;
    private String targetOptionCode;
    private String targetValue;
    private String description;

}
