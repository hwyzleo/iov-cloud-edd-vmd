package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleNodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleNodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleNodeQuery;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeCmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车载节点应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleNodeAppService {

    private final MdmVehicleNodeRepository mdmVehicleNodeRepository;

    /**
     * 查询车载节点信息
     *
     * @param query 查询 DTO
     * @return 车载节点 DTO 列表
     */
    public List<VehicleNodeDto> search(VehicleNodeQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("funcDomain", query.getFuncDomain());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<VehicleNode> vehicleNodeList = mdmVehicleNodeRepository.selectByMap(map);
        return PageUtil.convert(vehicleNodeList, VehicleNodeAssembler.INSTANCE::fromDomain);
    }

    /**
     * 获取所有车载节点
     *
     * @return 车载节点 DTO 列表
     */
    public List<VehicleNodeDto> listAll() {
        List<VehicleNode> vehicleNodeList = mdmVehicleNodeRepository.selectByMap(new HashMap<>());
        return VehicleNodeAssembler.INSTANCE.fromDomainList(vehicleNodeList);
    }

    /**
     * 检查车载节点代码是否唯一
     *
     * @param vehicleNodeId 车载节点ID
     * @param code          车载节点代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long vehicleNodeId, String code) {
        if (ObjUtil.isNull(vehicleNodeId)) {
            vehicleNodeId = -1L;
        }
        VehicleNode vehicleNode = mdmVehicleNodeRepository.selectByCode(code);
        return !ObjUtil.isNotNull(vehicleNode) || vehicleNode.getId().longValue() == vehicleNodeId.longValue();
    }

    /**
     * 根据主键ID获取车载节点信息
     *
     * @param id 主键ID
     * @return 车载节点 DTO
     */
    public VehicleNodeDto getVehicleNodeById(Long id) {
        return VehicleNodeAssembler.INSTANCE.fromDomain(mdmVehicleNodeRepository.selectById(id));
    }

    /**
     * 根据车载节点代码获取车载节点信息
     *
     * @param code 车载节点代码
     * @return 车载节点领域对象
     */
    public VehicleNode getVehicleNodeByCode(String code) {
        return mdmVehicleNodeRepository.selectByCode(code);
    }

    /**
     * 获取所有FOTA升级车载节点信息
     *
     * @return 车载节点信息列表
     */
    public List<VehicleNode> listAllFota() {
        Map<String, Object> map = new HashMap<>();
        map.put("otaSupport", "OTA");
        return mdmVehicleNodeRepository.selectByMap(map);
    }

    /**
     * 新增车载节点
     *
     * @param vehicleNodeCmd 车载节点命令
     * @return 结果
     */
    public int createVehicleNode(VehicleNodeCmd vehicleNodeCmd) {
        VehicleNode vehicleNode = VehicleNodeAssembler.INSTANCE.toDomain(vehicleNodeCmd);
        // 检查是否为 MDM 来源数据
        if (vehicleNode.getSource() == SourceType.MDM) {
            throw new ProductDataReadOnlyException("车载节点", vehicleNode.getCode());
        }
        return mdmVehicleNodeRepository.insert(vehicleNode);
    }

    /**
     * 修改车载节点
     *
     * @param vehicleNodeCmd 车载节点命令
     * @return 结果
     */
    public int modifyVehicleNode(VehicleNodeCmd vehicleNodeCmd) {
        VehicleNode vehicleNode = mdmVehicleNodeRepository.selectById(vehicleNodeCmd.getId());
        // 检查是否为 MDM 来源数据
        if (vehicleNode != null && vehicleNode.getSource() == SourceType.MDM) {
            throw new ProductDataReadOnlyException("车载节点", vehicleNode.getCode());
        }
        VehicleNode updateVehicleNode = VehicleNodeAssembler.INSTANCE.toDomain(vehicleNodeCmd);
        return mdmVehicleNodeRepository.update(updateVehicleNode);
    }

    /**
     * 批量删除车载节点
     *
     * @param ids 车载节点ID数组
     * @return 结果
     */
    public int deleteVehicleNodeByIds(Long[] ids) {
        // 检查是否为 MDM 来源数据
        for (Long id : ids) {
            VehicleNode vehicleNode = mdmVehicleNodeRepository.selectById(id);
            if (vehicleNode != null && vehicleNode.getSource() == SourceType.MDM) {
                throw new ProductDataReadOnlyException("车载节点", vehicleNode.getCode());
            }
        }
        return mdmVehicleNodeRepository.batchPhysicalDelete(ids);
    }

}
