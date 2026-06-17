package net.hwyz.iov.cloud.edd.vmd.service.application.vid;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ParserNotFoundException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 导入数据解析器注册表
 * <p>
 * 解析器启动时通过 {@link #register(VehicleImportDataParser)} 自注册，
 * 运行时通过 {@link #getParser(String, String)} 类型安全获取。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class ImportDataParserRegistry {

    private final ConcurrentHashMap<String, VehicleImportDataParser> registry = new ConcurrentHashMap<>();

    /**
     * 解析器自注册
     *
     * @param parser 解析器实例
     */
    public void register(VehicleImportDataParser parser) {
        String key = buildKey(parser.getType(), parser.getVersion());
        // PRODUCE 类型允许覆盖注册（US-040 新解析器替换旧解析器）
        if (isProduceType(parser.getType())) {
            registry.put(key, parser);
            log.info("注册导入数据解析器[{}] -> [{}]（覆盖注册）", key, parser.getClass().getSimpleName());
        } else {
            VehicleImportDataParser existing = registry.putIfAbsent(key, parser);
            if (existing != null) {
                log.warn("导入数据解析器[{}]已存在，跳过重复注册[{}]", key, parser.getClass().getSimpleName());
            } else {
                log.info("注册导入数据解析器[{}] -> [{}]", key, parser.getClass().getSimpleName());
            }
        }
    }

    /**
     * 判断是否为 PRODUCE 类型
     *
     * @param type 数据类型
     * @return 是否为 PRODUCE 类型
     */
    public boolean isProduceType(String type) {
        return "PRODUCE".equals(type);
    }

    /**
     * 获取解析器，不存在时抛出 {@link ParserNotFoundException}
     *
     * @param type    数据类型
     * @param version 版本号
     * @return 解析器实例
     * @throws ParserNotFoundException 解析器不存在
     */
    public VehicleImportDataParser getParser(String type, String version) {
        if (isProduceType(type)) {
            // PRODUCE 类型由 US-040 处理
            String key = "PRODUCE:" + version;
            VehicleImportDataParser parser = registry.get(key);
            if (parser == null) {
                throw new ParserNotFoundException(type, version);
            }
            return parser;
        }

        // 其他类型正常处理
        String key = buildKey(type, version);
        VehicleImportDataParser parser = registry.get(key);
        if (parser == null) {
            throw new ParserNotFoundException(type, version);
        }
        return parser;
    }

    private String buildKey(String type, String version) {
        return type.toUpperCase() + ":" + version;
    }

}
