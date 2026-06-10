package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.PartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartCmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 零件应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartAppService {

    private final PartRepository partRepository;

    /**
     * 查询零件信息
     *
     * @param query 查询 DTO
     * @return 零件 DTO 列表
     */
    public List<PartDto> search(PartQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("keyPart", query.getKey());
        map.put("pn", query.getPn());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("type", query.getType());
        map.put("deviceCode", query.getDeviceCode());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Part> partList = partRepository.selectByMap(map);
        return PageUtil.convert(partList, PartAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查零件号是否唯一
     *
     * @param partId 零件ID
     * @param pn     零件号
     * @return 结果
     */
    public Boolean checkPnUnique(Long partId, String pn) {
        if (ObjUtil.isNull(partId)) {
            partId = -1L;
        }
        Part part = partRepository.selectByPn(pn);
        return !ObjUtil.isNotNull(part) || part.getId().longValue() == partId.longValue();
    }

    /**
     * 根据主键ID获取零件信息
     *
     * @param id 主键ID
     * @return 零件 DTO
     */
    public PartDto getPartById(Long id) {
        return PartAssembler.INSTANCE.fromDomain(partRepository.selectById(id));
    }

    /**
     * 根据主键ID获取零件领域对象
     *
     * @param id 主键ID
     * @return 零件领域对象
     */
    public Part getPartEntityById(Long id) {
        return partRepository.selectById(id);
    }

    /**
     * 根据零件号获取零件信息
     *
     * @param pn 零件号
     * @return 零件领域对象
     */
    public Part getPartByPn(String pn) {
        return partRepository.selectByPn(pn);
    }

    /**
     * 获取所有FOTA升级零件信息
     *
     * @param software 是否是软件零件
     * @return 零件列表
     */
    public List<Part> listAllFota(Boolean software) {
        Map<String, Object> map = new HashMap<>();
        if (software != null) {
            map.put("naturePart", software ? 1 : 0);
        }
        return partRepository.selectByMap(map);
    }

    /**
     * 新增零件
     *
     * @param partDto 零件信息 DTO
     * @param userId 操作用户ID
     * @return 结果
     */
    public int createPart(PartCmd partCmd, String userId) {
        // CR-021: source=MDM 只读保护
        if (partCmd.getId() != null) {
            Part existingPart = partRepository.selectById(partCmd.getId());
            if (existingPart != null && SourceType.MDM.name().equals(existingPart.getSource())) {
                throw new RuntimeException("零件'" + partCmd.getPn() + "'来源为MDM，不允许通过VMD后台修改/删除");
            }
        }
        Part part = PartAssembler.INSTANCE.toDomain(partCmd);
        return partRepository.insert(part);
    }

    /**
     * 修改零件
     *
     * @param partDto 零件信息 DTO
     * @param userId 操作用户ID
     * @return 结果
     */
    public int modifyPart(PartCmd partCmd, String userId) {
        // CR-021: source=MDM 只读保护
        Part existingPart = partRepository.selectById(partCmd.getId());
        if (existingPart != null && SourceType.MDM.name().equals(existingPart.getSource())) {
            throw new RuntimeException("零件'" + partCmd.getPn() + "'来源为MDM，不允许通过VMD后台修改/删除");
        }
        Part part = PartAssembler.INSTANCE.toDomain(partCmd);
        return partRepository.update(part);
    }

    /**
     * 批量删除零件
     *
     * @param ids 零件ID数组
     * @return 结果
     */
    public int deletePartByIds(Long[] ids) {
        // CR-021: source=MDM 只读保护
        for (Long id : ids) {
            Part existingPart = partRepository.selectById(id);
            if (existingPart != null && SourceType.MDM.name().equals(existingPart.getSource())) {
                throw new RuntimeException("零件'" + existingPart.getPn() + "'来源为MDM，不允许通过VMD后台修改/删除");
            }
        }
        return partRepository.batchPhysicalDelete(ids);
    }

}
