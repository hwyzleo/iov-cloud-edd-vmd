package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 零件类型契约注册中心
 * <p>
 * 按part_type注册字段契约，供入站内核消费
 *
 * @author hwyz_leo
 */
@Component
public class PartTypeSchemaRegistry {

    private final Map<PartType, PartTypeSchema> schemas = new ConcurrentHashMap<>();

    public PartTypeSchemaRegistry() {
        // 注册默认契约
        registerDefaultSchemas();
    }

    private void registerDefaultSchemas() {
        // TBOX: 必需sn，可选iccid1/iccid2
        schemas.put(PartType.TBOX, PartTypeSchema.builder()
                .partType(PartType.TBOX)
                .requiredFields(List.of("sn"))
                .optionalFields(List.of("iccid1", "iccid2"))
                .defaultVehicleNodeCode("TBOX")
                .defaultDeviceItem("TBOX")
                .build());

        // BTM: 必需sn，可选hsm/mac，默认vehicleNodeCode=BTM_M
        schemas.put(PartType.BTM, PartTypeSchema.builder()
                .partType(PartType.BTM)
                .requiredFields(List.of("sn"))
                .optionalFields(List.of("hsm", "mac"))
                .defaultVehicleNodeCode("BTM_M")
                .defaultDeviceItem("BTM")
                .build());

        // CCP: 必需sn
        schemas.put(PartType.CCP, PartTypeSchema.builder()
                .partType(PartType.CCP)
                .requiredFields(List.of("sn"))
                .optionalFields(List.of())
                .defaultVehicleNodeCode("CCP")
                .defaultDeviceItem("CCP")
                .build());

        // IDCM: 必需sn
        schemas.put(PartType.IDCM, PartTypeSchema.builder()
                .partType(PartType.IDCM)
                .requiredFields(List.of("sn"))
                .optionalFields(List.of())
                .defaultVehicleNodeCode("IDCM")
                .defaultDeviceItem("IDCM")
                .build());

        // SIM: 必需iccid，可选imsi/msisdn/mno
        schemas.put(PartType.SIM, PartTypeSchema.builder()
                .partType(PartType.SIM)
                .requiredFields(List.of("iccid"))
                .optionalFields(List.of("imsi", "msisdn", "mno"))
                .build());

        // OTHER: 仅需sn
        schemas.put(PartType.OTHER, PartTypeSchema.builder()
                .partType(PartType.OTHER)
                .requiredFields(List.of("sn"))
                .optionalFields(List.of())
                .build());
    }

    /**
     * 获取零件类型契约
     *
     * @param partType 零件类型
     * @return 契约，不存在返回null
     */
    public PartTypeSchema getSchema(PartType partType) {
        return schemas.get(partType);
    }

    /**
     * 注册零件类型契约
     *
     * @param schema 契约
     */
    public void register(PartTypeSchema schema) {
        schemas.put(schema.getPartType(), schema);
    }
}
