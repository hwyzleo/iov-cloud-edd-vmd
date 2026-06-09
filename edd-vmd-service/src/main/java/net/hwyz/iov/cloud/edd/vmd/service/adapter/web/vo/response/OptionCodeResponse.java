package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 选装值响应 VO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionCodeResponse {

    private Long id;
    private String optionFamilyCode;
    private String code;
    private String name;
    private String nameEn;
    private String val;
    private Boolean enable;
    private Integer sort;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
