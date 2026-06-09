package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 选装族响应 VO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionFamilyResponse {

    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String type;
    private Boolean mandatory;
    private Boolean enable;
    private Integer sort;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
