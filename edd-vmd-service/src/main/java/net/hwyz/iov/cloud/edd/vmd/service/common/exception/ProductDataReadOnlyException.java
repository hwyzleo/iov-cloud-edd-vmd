package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 产品数据只读异常
 * 当 source=MDM 的记录被 MPT 写操作时抛出
 *
 * @author hwyz_leo
 */
public class ProductDataReadOnlyException extends VmdBaseException {

    public ProductDataReadOnlyException(String entity, String code) {
        super(VmdErrorCode.PRODUCT_DATA_READ_ONLY,
                String.format("%s'%s' 来源为 MDM，不允许通过 VMD 后台修改/删除", entity, code));
    }

}
