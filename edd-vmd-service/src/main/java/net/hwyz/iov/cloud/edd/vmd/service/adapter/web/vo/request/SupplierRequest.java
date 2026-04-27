package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台供应商
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SupplierRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 供应商代码
     */
    private String code;

    /**
     * 供应商名称
     */
    private String name;

    /**
     * 供应商简称
     */
    private String nameShort;

    /**
     * 供应商英文名称
     */
    private String nameEn;

    /**
     * 供应商类型
     */
    private Integer type;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String county;

    /**
     * 街道
     */
    private String subdistrict;

    /**
     * 地址
     */
    private String address;

    /**
     * 邮编
     */
    private String zipcode;

    /**
     * 供应商传真
     */
    private String fax;

    /**
     * 供应商电话
     */
    private String tel;

    /**
     * 供应商网站
     */
    private String website;

    /**
     * 供应商邮箱
     */
    private String email;

    /**
     * 供应商联系人
     */
    private String contactPerson;

    /**
     * 供应商联系人电话
     */
    private String contactPersonTel;

    /**
     * 供应商法人
     */
    private String legalPerson;

    /**
     * 供应商银行
     */
    private String bankName;

    /**
     * 供应商账号
     */
    private String accountNo;

    /**
     * 供应商税号
     */
    private String taxNo;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

}
