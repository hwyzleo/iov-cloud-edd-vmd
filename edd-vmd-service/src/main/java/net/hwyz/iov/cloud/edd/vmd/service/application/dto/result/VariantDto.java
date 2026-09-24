package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 版本 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantDto {

    private Long id;
    /**
     * 派生：平台代码（经 Model.platformCode，CR-048）
     */
    private String platformCode;
    /**
     * 派生：车系代码（经 Model.carLineCode，CR-048）
     */
    private String carLineCode;
    private String modelCode;
    private String code;
    private String name;
    private String nameLocal;
    private String description;
    private Instant createTime;

}
