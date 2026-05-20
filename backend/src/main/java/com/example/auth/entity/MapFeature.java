package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("map_feature")
public class MapFeature {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 要素类型: 1=点 2=线 3=面 */
    private Integer type;

    /** WKT 格式几何，如 POINT(116.3 39.9)、LINESTRING(...)、POLYGON((...)) */
    private String geometry;

    /** 名称 */
    private String name;

    /** 行政区划名称 */
    private String xzqhname;

    /** 行政区划代码 */
    private String code;

    /** 地址 */
    private String address;

    /** 备注 */
    private String remark;

    /** 创建人用户名 */
    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
