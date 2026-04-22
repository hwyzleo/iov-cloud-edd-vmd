package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 供应商表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-23
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_supplier")
public class VmdSupplierDo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 供应商代码
     */
    @TableField("code")
    private String code;

    /**
     * 供应商名称
     */
    @TableField("name")
    private String name;

    /**
     * 供应商简称
     */
    @TableField("name_short")
    private String nameShort;

    /**
     * 供应商英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 供应商类型
     */
    @TableField("type")
    private Integer type;

    /**
     * 省
     */
    @TableField("province")
    private String province;

    /**
     * 市
     */
    @TableField("city")
    private String city;

    /**
     * 区
     */
    @TableField("county")
    private String county;

    /**
     * 街道
     */
    @TableField("subdistrict")
    private String subdistrict;

    /**
     * 地址
     */
    @TableField("address")
    private String address;

    /**
     * 邮编
     */
    @TableField("zipcode")
    private String zipcode;

    /**
     * 供应商传真
     */
    @TableField("fax")
    private String fax;

    /**
     * 供应商电话
     */
    @TableField("tel")
    private String tel;

    /**
     * 供应商网站
     */
    @TableField("website")
    private String website;

    /**
     * 供应商邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 供应商联系人
     */
    @TableField("contact_person")
    private String contactPerson;

    /**
     * 供应商联系人电话
     */
    @TableField("contact_person_tel")
    private String contactPersonTel;

    /**
     * 供应商法人
     */
    @TableField("legal_person")
    private String legalPerson;

    /**
     * 供应商银行
     */
    @TableField("bank_name")
    private String bankName;

    /**
     * 供应商账号
     */
    @TableField("account_no")
    private String accountNo;

    /**
     * 供应商税号
     */
    @TableField("tax_no")
    private String taxNo;

    /**
     * 是否启用
     */
    @TableField("enable")
    private Boolean enable;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

}
