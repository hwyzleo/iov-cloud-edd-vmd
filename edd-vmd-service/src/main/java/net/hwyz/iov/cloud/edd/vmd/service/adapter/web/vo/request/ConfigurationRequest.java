package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigurationRequest extends BaseRequest {

    private Long id;

    /**
     * 版本代码（CR-047：Configuration 唯一直接产品树父引用）
     */
    private String variantCode;

    private String code;

    private String name;

    /**
     * 本地化名称（CR-047）
     */
    private String nameLocal;

    private Date createTime;

}
