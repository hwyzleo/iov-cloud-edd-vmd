package net.hwyz.iov.cloud.edd.vmd.service.application.vid;

import cn.hutool.json.JSONObject;

/**
 * 导入数据解析器
 *
 * @author hwyz_leo
 */
public interface ImportDataParser {

    /**
     * 获取数据类型（如 PRODUCE、EOL、IDCM）
     *
     * @return 数据类型，大写
     */
    String getType();

    /**
     * 获取解析器版本号（如 1.0、2.0）
     *
     * @return 版本号
     */
    String getVersion();

    /**
     * 解析数据
     *
     * @param batchNum 批次号
     * @param dataJson 数据JSON
     */
    void parse(String batchNum, JSONObject dataJson);

}
