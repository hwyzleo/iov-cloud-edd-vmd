package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台零件导入数据请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PartImportDataRequest extends BaseRequest {

    private Long id;
    private String batchNum;
    private String partCode;
    private String version;
    private String data;
    private Boolean handle;
    private String description;
    private Date createTime;
    private Date beginTime;
    private Date endTime;
}
