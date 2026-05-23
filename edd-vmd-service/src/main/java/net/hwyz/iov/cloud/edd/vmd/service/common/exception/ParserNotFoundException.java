package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 导入数据解析器不存在异常
 *
 * @author hwyz_leo
 */
public class ParserNotFoundException extends VmdBaseException {

    public ParserNotFoundException(String type, String version) {
        super(VmdErrorCode.PARSER_NOT_FOUND,
                String.format("不支持的数据类型[%s]版本[%s]", type, version));
    }

}
