package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.CompensateCertificateRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleCertificateQueryRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateCompensateResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateDetailResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateListResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.CertificateOperationResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CompensateCertificateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleCertificateQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateCompensateResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateDetailResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateListResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateOperationResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MPT 证书申请补偿对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptVehicleCertificateAssembler {

    MptVehicleCertificateAssembler INSTANCE = Mappers.getMapper(MptVehicleCertificateAssembler.class);

    /**
     * 查询请求 → 查询条件
     */
    VehicleCertificateQuery toQuery(VehicleCertificateQueryRequest request);

    /**
     * 补偿请求 → 补偿命令
     */
    CompensateCertificateCmd toCmd(CompensateCertificateRequest request);

    /**
     * 补偿结果 → 补偿响应
     */
    CertificateCompensateResponse toCompensateResponse(CertificateCompensateResult result);

    /**
     * 列表结果 → 列表响应
     */
    CertificateListResponse toListResponse(CertificateListResult result);

    /**
     * 列表结果集合 → 列表响应集合
     */
    List<CertificateListResponse> toListResponseList(List<CertificateListResult> results);

    /**
     * 详情结果 → 详情响应
     */
    CertificateDetailResponse toDetailResponse(CertificateDetailResult result);

    /**
     * 操作审计结果 → 操作审计响应
     */
    List<CertificateOperationResponse> toOperationResponseList(List<CertificateOperationResult> results);

}
