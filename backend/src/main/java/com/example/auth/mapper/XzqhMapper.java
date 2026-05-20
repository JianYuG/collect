package com.example.auth.mapper;

import com.example.auth.entity.XzqhItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface XzqhMapper {

    /**
     * 查询 1-4 级行政区划（省/市/区县/乡镇），附带 bbox 和 centroid
     * bbox 格式: "minX,minY,maxX,maxY"
     * centroid 格式: "lon,lat"
     */
    @Select("SELECT id, name, fullname, code, level, " +
            "  CONCAT(" +
            "    ST_XMin(ST_Envelope(geom)), ',', " +
            "    ST_YMin(ST_Envelope(geom)), ',', " +
            "    ST_XMax(ST_Envelope(geom)), ',', " +
            "    ST_YMax(ST_Envelope(geom))" +
            "  ) AS bbox, " +
            "  CONCAT(ST_X(ST_Centroid(geom)), ',', ST_Y(ST_Centroid(geom))) AS centroid " +
            "FROM xzqh " +
            "WHERE level <= 4 " +
            "ORDER BY level, code")
    List<XzqhItem> listLevels1to4();
}
