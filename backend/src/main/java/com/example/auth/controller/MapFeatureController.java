package com.example.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.auth.entity.MapFeature;
import com.example.auth.exception.Result;
import com.example.auth.mapper.MapFeatureMapper;
import com.example.auth.service.MapFeatureService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/features")
public class MapFeatureController {

    @Autowired
    private MapFeatureService mapFeatureService;

    @Autowired
    private MapFeatureMapper mapFeatureMapper;

    /**
     * 查询所有地图要素（按 type 分组显示用）
     * GET /api/features/all
     */
    @GetMapping("/all")
    public Result<List<MapFeature>> listAll() {
        List<MapFeature> list = mapFeatureMapper.selectList(
                new QueryWrapper<MapFeature>().orderByAsc("type", "id")
        );
        return Result.success(list);
    }

    /**
     * 新增地图要素
     * POST /api/features
     * Body: { type, geometry, name, xzqh, address, remark }
     */
    @PostMapping
    public Result<MapFeature> create(@Validated @RequestBody CreateFeatureRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";

        MapFeature feature = new MapFeature();
        feature.setType(req.getType());
        feature.setGeometry(req.getGeometry());
        feature.setName(req.getName());
        feature.setXzqhname(req.getXzqhname());
        feature.setCode(req.getCode());
        feature.setAddress(req.getAddress());
        feature.setRemark(req.getRemark());

        MapFeature saved = mapFeatureService.save(feature, username);
        return Result.success("保存成功", saved);
    }

    @Data
    public static class CreateFeatureRequest {
        /** 要素类型: 1=点 2=线 3=面 */
        @NotNull(message = "type 不能为空")
        @Min(value = 1, message = "type 最小为 1")
        @Max(value = 3, message = "type 最大为 3")
        private Integer type;

        /** WKT 格式几何字符串 */
        @NotBlank(message = "geometry 不能为空")
        private String geometry;

        private String name;
        private String xzqhname;  // 行政区划名称
        private String code;      // 行政区划代码
        private String address;
        private String remark;
    }
}
