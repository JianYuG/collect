package com.example.auth.entity;

import lombok.Data;

/**
 * 行政区划查询结果 DTO
 * 不映射数据库表（非 @TableName），仅用于接收 MyBatis 查询结果
 */
@Data
public class XzqhItem {

    private Integer id;

    /** 简称，如"杭州市" */
    private String name;

    /** 全称，如"浙江省杭州市" */
    private String fullname;

    /** 行政区划代码 */
    private String code;

    /** 层级: 1=省 2=市 3=区县 4=乡镇街道 */
    private Integer level;

    /**
     * 包围盒，格式: "minX,minY,maxX,maxY"（EPSG:4490 经纬度）
     * 前端用于 fit 地图视图
     */
    private String bbox;

    /**
     * 质心坐标，格式: "lon,lat"
     * 用于点级别定位
     */
    private String centroid;
}
