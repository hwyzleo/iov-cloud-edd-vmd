package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台车辆导入数据
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class VehicleImportDataResponse {

    /**
     * 主键
     */
    private Long id;

    /**
     * 批次号
     */
    private String batchNum;

    /**
     * 数据类型
     */
    private String type;

    /**
     * 数据版本
     */
    private String version;

    /**
     * MES车辆数据
     */
    private String data;

    /**
     * 是否处理
     */
    private Boolean handle;

    /**
     * 备注
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
