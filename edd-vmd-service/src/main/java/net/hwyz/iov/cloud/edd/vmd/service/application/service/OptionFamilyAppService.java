package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.OptionCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.OptionFamilyAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OptionCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OptionFamilyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.OptionCodeQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.OptionFamilyQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehOptionFamilyRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 选装应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptionFamilyAppService {

    private final VehOptionFamilyRepository vehOptionFamilyRepository;

    // ==================== 选装族 ====================

    /**
     * 查询选装族信息
     *
     * @param query 查询 DTO
     * @return 选装族列表
     */
    public List<OptionFamilyDto> search(OptionFamilyQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("type", query.getType());
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<OptionFamily> optionFamilyList = vehOptionFamilyRepository.selectByMap(map);
        return PageUtil.convert(optionFamilyList, OptionFamilyAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查选装族代码是否唯一
     *
     * @param optionFamilyId 选装族ID
     * @param code           选装族代码
     * @return 结果
     */
    public Boolean checkOptionFamilyCodeUnique(Long optionFamilyId, String code) {
        if (ObjUtil.isNull(optionFamilyId)) {
            optionFamilyId = -1L;
        }
        OptionFamily optionFamily = vehOptionFamilyRepository.selectByCode(code);
        return !ObjUtil.isNotNull(optionFamily) || optionFamily.getId().longValue() == optionFamilyId.longValue();
    }

    /**
     * 检查选装值代码是否唯一
     *
     * @param optionCodeId 选装值ID
     * @param code         选装值代码
     * @return 结果
     */
    public Boolean checkOptionCodeUnique(Long optionCodeId, String code) {
        if (ObjUtil.isNull(optionCodeId)) {
            optionCodeId = -1L;
        }
        OptionCode optionCode = vehOptionFamilyRepository.selectOptionCodeByCode(code);
        return !ObjUtil.isNotNull(optionCode) || optionCode.getId().longValue() == optionCodeId.longValue();
    }

    /**
     * 根据主键ID获取选装族信息
     *
     * @param id 主键ID
     * @return 选装族 DTO
     */
    public OptionFamilyDto getOptionFamilyById(Long id) {
        return OptionFamilyAssembler.INSTANCE.fromDomain(vehOptionFamilyRepository.selectById(id));
    }

    /**
     * 根据选装族代码获取选装族信息
     *
     * @param code 选装族代码
     * @return 选装族 DTO
     */
    public OptionFamilyDto getOptionFamilyByCode(String code) {
        return OptionFamilyAssembler.INSTANCE.fromDomain(vehOptionFamilyRepository.selectByCode(code));
    }

    /**
     * 新增选装族
     *
     * @param optionFamilyCmd 选装族命令 DTO
     * @param userId          操作用户ID
     * @return 结果
     */
    public int createOptionFamily(OptionFamilyCmd optionFamilyCmd, String userId) {
        OptionFamily optionFamilyDomain = OptionFamilyAssembler.INSTANCE.toDomain(optionFamilyCmd);
        return vehOptionFamilyRepository.insert(optionFamilyDomain);
    }

    /**
     * 修改选装族
     *
     * @param optionFamilyCmd 选装族命令 DTO
     * @param userId          操作用户ID
     * @return 结果
     */
    public int modifyOptionFamily(OptionFamilyCmd optionFamilyCmd, String userId) {
        OptionFamily optionFamily = vehOptionFamilyRepository.selectById(optionFamilyCmd.getId());
        if ("MDM".equals(optionFamily.getSource())) {
            throw new ProductDataReadOnlyException("选装族", optionFamily.getCode());
        }
        OptionFamily updateDomain = OptionFamilyAssembler.INSTANCE.toDomain(optionFamilyCmd);
        return vehOptionFamilyRepository.update(updateDomain);
    }

    /**
     * 批量删除选装族
     *
     * @param ids 选装族ID数组
     * @return 结果
     */
    public int deleteOptionFamilyByIds(Long[] ids) {
        for (Long id : ids) {
            OptionFamily optionFamily = vehOptionFamilyRepository.selectById(id);
            if (optionFamily != null && "MDM".equals(optionFamily.getSource())) {
                throw new ProductDataReadOnlyException("选装族", optionFamily.getCode());
            }
        }
        return vehOptionFamilyRepository.batchPhysicalDelete(ids);
    }

    // ==================== 选装值 ====================

    /**
     * 查询选装值信息
     *
     * @param query 查询 DTO
     * @return 选装值列表
     */
    public List<OptionCodeDto> searchOptionCode(OptionCodeQuery query) {
        String familyCode = query.getOptionFamilyCode();
        if (ObjUtil.isNotNull(query.getOptionFamilyId())) {
            OptionFamily optionFamily = vehOptionFamilyRepository.selectById(query.getOptionFamilyId());
            if (optionFamily != null) {
                familyCode = optionFamily.getCode();
            }
        }
        List<OptionCode> list = vehOptionFamilyRepository.selectOptionCodeByOptionFamilyCode(familyCode);
        return PageUtil.convert(list, OptionCodeAssembler.INSTANCE::fromDomain);
    }

    /**
     * 根据主键ID获取选装值信息
     *
     * @param optionFamilyId 选装族ID
     * @param id             选装值ID
     * @return 选装值 DTO
     */
    public OptionCodeDto getOptionCodeById(Long optionFamilyId, Long id) {
        OptionCode optionCode = vehOptionFamilyRepository.selectOptionCodeById(id);
        return OptionCodeAssembler.INSTANCE.fromDomain(optionCode);
    }

    /**
     * 根据选装值代码获取选装值信息
     *
     * @param code 选装值代码
     * @return 选装值 DTO
     */
    public OptionCodeDto getOptionCodeByCode(String code) {
        return OptionCodeAssembler.INSTANCE.fromDomain(vehOptionFamilyRepository.selectOptionCodeByCode(code));
    }

    /**
     * 根据选装族代码获取所有选装值
     *
     * @param optionFamilyCode 选装族代码
     * @return 选装值列表
     */
    public List<OptionCodeDto> listAllOptionCodeByOptionFamilyCode(String optionFamilyCode) {
        List<OptionCode> list = vehOptionFamilyRepository.selectOptionCodeByOptionFamilyCode(optionFamilyCode);
        return PageUtil.convert(list, OptionCodeAssembler.INSTANCE::fromDomain);
    }

    /**
     * 新增选装值
     *
     * @param familyId      选装族ID
     * @param optionCodeCmd 选装值命令 DTO
     * @param userId        操作用户ID
     * @return 结果
     */
    public int createOptionCode(Long familyId, OptionCodeCmd optionCodeCmd, String userId) {
        OptionCode optionCodeDomain = OptionCodeAssembler.INSTANCE.toDomain(optionCodeCmd);
        optionCodeDomain.setOptionFamilyCode(vehOptionFamilyRepository.selectById(familyId).getCode());
        return vehOptionFamilyRepository.insertOptionCode(optionCodeDomain);
    }

    /**
     * 修改选装值
     *
     * @param optionFamilyId 选装族ID
     * @param optionCodeCmd  选装值命令 DTO
     * @param userId         操作用户ID
     * @return 结果
     */
    public int modifyOptionCode(Long optionFamilyId, OptionCodeCmd optionCodeCmd, String userId) {
        OptionCode optionCode = vehOptionFamilyRepository.selectOptionCodeById(optionCodeCmd.getId());
        if ("MDM".equals(optionCode.getSource())) {
            throw new ProductDataReadOnlyException("选装值", optionCode.getCode());
        }
        OptionCode updateDomain = OptionCodeAssembler.INSTANCE.toDomain(optionCodeCmd);
        return vehOptionFamilyRepository.updateOptionCode(updateDomain);
    }

    /**
     * 批量删除选装值
     *
     * @param optionFamilyId 选装族ID
     * @param ids            选装值ID数组
     * @return 结果
     */
    public int deleteOptionCodeByIds(Long optionFamilyId, Long[] ids) {
        for (Long id : ids) {
            OptionCode optionCode = vehOptionFamilyRepository.selectOptionCodeById(id);
            if (optionCode != null && "MDM".equals(optionCode.getSource())) {
                throw new ProductDataReadOnlyException("选装值", optionCode.getCode());
            }
        }
        return vehOptionFamilyRepository.batchPhysicalDeleteOptionCode(ids);
    }

}
