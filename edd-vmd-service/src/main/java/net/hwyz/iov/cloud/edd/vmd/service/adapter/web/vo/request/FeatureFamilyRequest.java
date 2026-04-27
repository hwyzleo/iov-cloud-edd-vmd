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
public class FeatureFamilyRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 特征族代码
     */
    private String code;

    /**
     * 特征族名称
     */
    private String name;

    /**
     * 特征族英文名称
     */
    private String nameEn;

    /**
     * 特征族分类
     */
    private String type;

    /**
     * 是否强制
     */
    private Boolean mandatory;

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
