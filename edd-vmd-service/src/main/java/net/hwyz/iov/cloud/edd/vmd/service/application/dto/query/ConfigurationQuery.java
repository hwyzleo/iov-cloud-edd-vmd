package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 生产配置查询 DTO
 * <p>
 * platformCode/carLineCode/modelCode 为产品树筛选条件（数据库侧 JOIN 完成，CR-047），
 * 不再读取 Configuration 冗余列。
 * </p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class ConfigurationQuery {

    private String platformCode;
    private String carLineCode;
    private String modelCode;
    private String variantCode;
    private String code;
    private String name;
    private Date beginTime;
    private Date endTime;

}
