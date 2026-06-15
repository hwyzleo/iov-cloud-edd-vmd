package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 零件导入数据异常
 *
 * @author hwyz_leo
 */
public class PartImportDataException extends VmdBaseException {

    public PartImportDataException(String message) {
        super(VmdErrorCode.PART_IMPORT_DATA_EXCEPTION, message);
    }
}
