package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.PartInfoAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartInfoDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PartInfoQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoCmd;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物理零件实例应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartInfoAppService {

    private final PartInfoRepository partInfoRepository;

    /**
     * 查询物理零件实例信息
     *
     * @param query 查询 DTO
     * @return 物理零件实例 DTO 列表
     */
    public List<PartInfoDto> search(PartInfoQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("partCode", query.getPartCode());
        map.put("sn", query.getSn());
        map.put("vehicleNodeCode", query.getVehicleNodeCode());
        map.put("instanceState", query.getInstanceState());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        // CR-023: 入站特有查询条件
        map.put("source", query.getSource());
        map.put("partType", query.getPartType());
        map.put("inboundBatchNo", query.getInboundBatchNo());
        List<PartInfo> partInfoList = partInfoRepository.selectByMap(map);
        return PageUtil.convert(partInfoList, PartInfoAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查零件编码和序列号是否唯一
     *
     * @param id 主键ID
     * @param partCode 零件编码
     * @param sn 序列号
     * @return 结果
     */
    public Boolean checkPartCodeAndSnUnique(Long id, String partCode, String sn) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        PartInfo partInfo = partInfoRepository.selectByPartCodeAndSn(partCode, sn);
        return !ObjUtil.isNotNull(partInfo) || partInfo.getId().longValue() == id.longValue();
    }

    /**
     * 根据主键ID获取物理零件实例信息
     *
     * @param id 主键ID
     * @return 物理零件实例 DTO
     */
    public PartInfoDto getPartInfoById(Long id) {
        return PartInfoAssembler.INSTANCE.fromDomain(partInfoRepository.selectById(id));
    }

    /**
     * 根据零件编码和序列号获取物理零件实例信息
     *
     * @param partCode 零件编码
     * @param sn 序列号
     * @return 物理零件实例 DTO
     */
    public PartInfoDto getPartInfoByPartCodeAndSn(String partCode, String sn) {
        return PartInfoAssembler.INSTANCE.fromDomain(partInfoRepository.selectByPartCodeAndSn(partCode, sn));
    }

    /**
     * 创建物理零件实例
     *
     * @param partInfoList 物理零件实例列表
     * @return 结果
     */
    public int createPartInfo(List<PartInfo> partInfoList) {
        return partInfoRepository.batchInsert(partInfoList);
    }

    /**
     * 新增物理零件实例
     *
     * @param partInfoCmd 物理零件实例信息 DTO
     * @param userId 操作用户ID
     * @return 结果
     */
    public int createPartInfo(PartInfoCmd partInfoCmd, String userId) {
        PartInfo partInfo = PartInfoAssembler.INSTANCE.toDomain(partInfoCmd);
        partInfo.setFirstSeenTime(Instant.now());
        return partInfoRepository.insert(partInfo);
    }

    /**
     * 修改物理零件实例
     *
     * @param partInfoCmd 物理零件实例信息 DTO
     * @param userId 操作用户ID
     * @return 结果
     */
    public int modifyPartInfo(PartInfoCmd partInfoCmd, String userId) {
        PartInfo partInfo = PartInfoAssembler.INSTANCE.toDomain(partInfoCmd);
        return partInfoRepository.update(partInfo);
    }

    /**
     * 批量删除物理零件实例
     *
     * @param ids 主键ID数组
     * @return 结果
     */
    public int deletePartInfoByIds(Long[] ids) {
        return partInfoRepository.batchPhysicalDelete(ids);
    }

    /**
     * Upsert 物理零件实例（幂等）
     * 如果存在则更新，否则新增
     *
     * @param partInfo 物理零件实例
     * @return 结果
     */
    public int upsertPartInfo(PartInfo partInfo) {
        PartInfo existing = partInfoRepository.selectByPartCodeAndSn(partInfo.getPartCode(), partInfo.getSn());
        if (existing != null) {
            partInfo.setId(existing.getId());
            return partInfoRepository.update(partInfo);
        } else {
            partInfo.setFirstSeenTime(Instant.now());
            return partInfoRepository.insert(partInfo);
        }
    }

    /**
     * 回填零件细节（软件版本、配置字/变体编码等）入 part_info.extra
     *
     * @param partCode 零件编码
     * @param sn 零件序列号
     * @param extraFields 额外字段
     * @return 结果
     */
    public int updateExtraFields(String partCode, String sn, Map<String, String> extraFields) {
        PartInfo existing = partInfoRepository.selectByPartCodeAndSn(partCode, sn);
        if (existing == null) {
            log.warn("零件[{}]序列号[{}]不存在，无法更新额外字段", partCode, sn);
            return 0;
        }
        // 合并现有 extra 字段
        Map<String, Object> extraMap = new HashMap<>();
        if (existing.getExtra() != null && !existing.getExtra().isEmpty()) {
            extraMap.putAll(cn.hutool.json.JSONUtil.parseObj(existing.getExtra()));
        }
        extraMap.putAll(extraFields);
        existing.setExtra(cn.hutool.json.JSONUtil.toJsonStr(extraMap));
        return partInfoRepository.update(existing);
    }

}
