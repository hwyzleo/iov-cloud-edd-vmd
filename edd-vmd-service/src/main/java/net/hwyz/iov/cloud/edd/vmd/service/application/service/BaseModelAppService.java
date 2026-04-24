package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelFeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BaseModelFeatureCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BaseModelAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBaseModelMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBaseModelFeatureCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBasicInfoMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBuildConfigMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelFeatureCodePo;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础车型应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseModelAppService {

    private final VehBasicInfoMapper vehBasicInfoMapper;
    private final VehBaseModelMapper vehBaseModelMapper;
    private final VehBuildConfigMapper vehBuildConfigMapper;
    private final VehBaseModelFeatureCodeMapper vehBaseModelFeatureCodeAssembler;

    /**
     * 查询基础车型信息
     *
     * @param platformCode 车辆平台代码
     * @param seriesCode   车系代码
     * @param modelCode    车型代码
     * @param code         基础车型代码
     * @param name         基础车型名称
     * @param beginTime    开始时间
     * @param endTime      结束时间
     * @return 基础车型列表
     */
    public List<BaseModelVo> search(String platformCode, String seriesCode, String modelCode, String code, String name,
                                    Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformCode);
        map.put("seriesCode", seriesCode);
        map.put("modelCode", modelCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VehBaseModelPo> vehBaseModelPoList = vehBaseModelMapper.selectPoByMap(map);
        return PageUtil.convert(vehBaseModelPoList, BaseModelAssembler.INSTANCE::fromPo);
    }

    /**
     * 查询基础车型信息
     *
     * @param baseModelCode 基础车型代码
     * @param familyCode    特征族代码
     * @param beginTime     开始时间
     * @param endTime       结束时间
     * @return 基础车型列表
     */
    public List<BaseModelFeatureCodeVo> searchFeatureCode(String baseModelCode, String familyCode, Date beginTime,
                                                          Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("baseModelCode", baseModelCode);
        map.put("familyCode", familyCode);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return BaseModelFeatureCodeAssembler.INSTANCE.fromPoList(vehBaseModelFeatureCodeAssembler.selectPoByMap(map));
    }

    /**
     * 检查基础车型代码是否唯一
     *
     * @param baseModelId 基础车型ID
     * @param code        基础车型代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long baseModelId, String code) {
        if (ObjUtil.isNull(baseModelId)) {
            baseModelId = -1L;
        }
        VehBaseModelPo baseModelPo = getBaseModelByCode(code);
        return !ObjUtil.isNotNull(baseModelPo) || baseModelPo.getId().longValue() == baseModelId.longValue();
    }

    /**
     * 检查基础车型特征值代码是否唯一
     *
     * @param baseModelFeatureCodeId 基础车型特征值ID
     * @param baseModelCode          基础车型代码
     * @param familyCode             特征族代码
     * @return 结果
     */
    public Boolean checkFeatureCodeUnique(Long baseModelFeatureCodeId, String baseModelCode, String familyCode) {
        if (ObjUtil.isNull(baseModelFeatureCodeId)) {
            baseModelFeatureCodeId = -1L;
        }
        VehBaseModelFeatureCodePo baseModelFeatureCodePo = getBaseModelFeatureCodeByCode(baseModelCode, familyCode);
        return !ObjUtil.isNotNull(baseModelFeatureCodePo) || baseModelFeatureCodePo.getId().longValue() == baseModelFeatureCodeId.longValue();
    }

    /**
     * 检查基础车型下是否存在生产配置
     *
     * @param baseModelId 基础车型ID
     * @return 结果
     */
    public Boolean checkBaseModelBuildConfigExist(Long baseModelId) {
        VehBaseModelPo baseModelPo = getBaseModelById(baseModelId);
        Map<String, Object> map = new HashMap<>();
        map.put("baseModelCode", baseModelPo.getCode());
        return vehBuildConfigMapper.countPoByMap(map) > 0;
    }

    /**
     * 检查基础车型下是否存在车辆
     *
     * @param baseModelId 基础车型ID
     * @return 结果
     */
    public Boolean checkBaseModelVehicleExist(Long baseModelId) {
        VehBaseModelPo baseModelPo = getBaseModelById(baseModelId);
        Map<String, Object> map = new HashMap<>();
        map.put("baseModelCode", baseModelPo.getCode());
        return vehBasicInfoMapper.countPoByMap(map) > 0;
    }

    /**
     * 根据主键ID获取基础车型信息
     *
     * @param id 主键ID
     * @return 基础车型信息
     */
    public VehBaseModelPo getBaseModelById(Long id) {
        return vehBaseModelMapper.selectPoById(id);
    }

    /**
     * 根据主键ID获取基础车型信息
     *
     * @param id 主键ID
     * @return 基础车型信息
     */
    public BaseModelFeatureCodeVo getBaseModelFeatureCodeById(Long id) {
        return BaseModelFeatureCodeAssembler.INSTANCE.fromPo(vehBaseModelFeatureCodeAssembler.selectPoById(id));
    }

    /**
     * 根据基础车型代码获取基础车型信息
     *
     * @param code 基础车型代码
     * @return 基础车型信息
     */
    public VehBaseModelPo getBaseModelByCode(String code) {
        return vehBaseModelMapper.selectPoByCode(code);
    }

    /**
     * 根据基础车型代码获取基础车型信息
     *
     * @param baseModelCode 基础车型代码
     * @param familyCode    特征族代码
     * @return 基础车型特征值信息
     */
    public VehBaseModelFeatureCodePo getBaseModelFeatureCodeByCode(String baseModelCode, String familyCode) {
        return vehBaseModelFeatureCodeAssembler.selectPoByBaseModelCodeAndFamilyCode(baseModelCode, familyCode);
    }

    /**
     * 新增基础车型
     *
     * @param baseModel 基础车型信息
     * @return 结果
     */
    public int createBasicModel(VehBaseModelPo baseModel) {
        return vehBaseModelMapper.insertPo(baseModel);
    }

    /**
     * 新增基础车型
     *
     * @param baseModelFeatureCode 基础车型特征值
     * @return 结果
     */
    public int createBasicModelFeatureCode(VehBaseModelFeatureCodePo baseModelFeatureCode) {
        return vehBaseModelFeatureCodeAssembler.insertPo(baseModelFeatureCode);
    }

    /**
     * 修改基础车型
     *
     * @param baseModel 基础车型信息
     * @return 结果
     */
    public int modifyBasicModel(VehBaseModelPo baseModel) {
        return vehBaseModelMapper.updatePo(baseModel);
    }

    /**
     * 修改基础车型特征值
     *
     * @param baseModelFeatureCode 基础车型特征值
     * @return 结果
     */
    public int modifyBaseModelFeatureCode(VehBaseModelFeatureCodePo baseModelFeatureCode) {
        return vehBaseModelFeatureCodeAssembler.updatePo(baseModelFeatureCode);
    }

    /**
     * 批量删除基础车型
     *
     * @param ids 基础车型ID数组
     * @return 结果
     */
    public int deleteBasicModelByIds(Long[] ids) {
        return vehBaseModelMapper.batchPhysicalDeletePo(ids);
    }

    /**
     * 批量删除基础车型
     *
     * @param ids 基础车型ID数组
     * @return 结果
     */
    public int deleteBaseModelFeatureCodeByIds(Long[] ids) {
        return vehBaseModelFeatureCodeAssembler.batchPhysicalDeletePo(ids);
    }

}
