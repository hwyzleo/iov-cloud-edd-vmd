package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车系 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesDto {

    private Long id;
    private String platformCode;
    private String code;
    private String name;
    private String nameEn;
    private Boolean enable;
    private Integer sort;
    private String description;

}
