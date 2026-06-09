package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 选装值查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class OptionCodeQuery {

    private Long optionFamilyId;
    private String optionFamilyCode;
    private String name;
    private String optionCode;
    private Date beginTime;
    private Date endTime;

}
