package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 供应商查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class SupplierQuery {

    private String code;
    private String name;
    private Date beginTime;
    private Date endTime;

}
