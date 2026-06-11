package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 未登记的零件类型字段契约异常
 *
 * @author hwyz_leo
 */
public class PartTypeSchemaNotFoundException extends VmdBaseException {

    public PartTypeSchemaNotFoundException(String partType) {
        super(VmdErrorCode.PART_TYPE_SCHEMA_NOT_FOUND, "未登记的零件类型字段契约[" + partType + "]");
    }
}
