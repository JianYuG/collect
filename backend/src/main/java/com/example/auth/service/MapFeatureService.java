package com.example.auth.service;

import com.example.auth.entity.MapFeature;
import com.example.auth.exception.BusinessException;
import com.example.auth.mapper.MapFeatureMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MapFeatureService {

    @Autowired
    private MapFeatureMapper mapFeatureMapper;

    /**
     * 保存地图要素
     *
     * @param feature  要素实体（type 1/2/3、WKT geometry、属性字段已填充）
     * @param username 当前登录用户名
     * @return 保存后的实体（含自增 id）
     */
    public MapFeature save(MapFeature feature, String username) {
        if (feature.getType() == null) {
            throw new BusinessException(400, "type 不能为空");
        }
        if (feature.getType() < 1 || feature.getType() > 3) {
            throw new BusinessException(400, "type 只允许 1(点)/2(线)/3(面)");
        }
        if (feature.getGeometry() == null || feature.getGeometry().trim().isEmpty()) {
            throw new BusinessException(400, "geometry 不能为空");
        }
        feature.setCreatedBy(username);
        mapFeatureMapper.insert(feature);
        log.info("保存地图要素成功: id={}, type={}, user={}",
                feature.getId(), feature.getType(), username);
        return feature;
    }
}
