package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入处理结果响应
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultResponse {

    /**
     * 总记录数
     */
    private int totalCount;

    /**
     * 成功记录数
     */
    private int successCount;

    /**
     * 失败记录数
     */
    private int failureCount;

    /**
     * 无效记录数（校验不通过被跳过）
     */
    private int invalidCount;

}
