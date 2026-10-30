package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 零件实例车载节点冲突异常（CR-072）
 * <p>
 * 当前实例节点与输入节点均非空且不同：普通入站/合法绑定/重导一律拒绝本次记录变更，
 * 不借 deviceCategory 纠错换节点；该项不部分提交（节点/类别/其他字段均保留原值），不影响其他记录。
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
@Getter
@Slf4j
public class PartNodeConflictException extends VmdBaseException {

    private final String partCode;
    private final String sn;

    public PartNodeConflictException(String partCode, String sn, String currentNode, String inputNode) {
        super(VmdErrorCode.DEVICE_CATEGORY_NODE_CONFLICT,
                "零件[" + partCode + ":" + sn + "]当前节点[" + currentNode + "]与输入节点[" + inputNode + "]冲突");
        this.partCode = partCode;
        this.sn = sn;
        log.warn("零件[{}:{}]节点冲突: current={}, input={}", partCode, sn, currentNode, inputNode);
    }

}
