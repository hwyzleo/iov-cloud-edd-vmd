package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 选装族请求 VO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OptionFamilyRequest extends BaseRequest {

    private Long id;
    private String code;
    private String name;
    private String nameLocal;
    private String type;

}
