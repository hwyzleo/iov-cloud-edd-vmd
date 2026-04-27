package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车辆特征族
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FeatureCodeRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 特征族代码
     */
    private String familyCode;

    /**
     * 特征值代码
     */
    private String code;

    /**
     * 特征值名称
     */
    private String name;

    /**
     * 特征值英文名称
     */
    private String nameEn;

    /**
     * 特征值代表值
     */
    private String val;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

}
