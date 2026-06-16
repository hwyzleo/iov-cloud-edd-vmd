package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 选装值 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionCodeDto {

    private Long id;
    private String optionFamilyCode;
    private String code;
    private String name;
    private String nameLocal;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
