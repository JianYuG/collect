-- 删除旧表，重建符合 PostGIS/Shapefile 导出规范的新结构
DROP TABLE IF EXISTS map_feature;

CREATE TABLE map_feature (
    id          BIGSERIAL    PRIMARY KEY,
    type        SMALLINT     NOT NULL,           -- 要素类型: 1=点 2=线 3=面
    geometry    TEXT         NOT NULL,           -- WKT 格式几何（PostGIS 可直接转 geometry）
    name        VARCHAR(100),                    -- 名称
    xzqh        VARCHAR(100),                    -- 行政区划
    address     VARCHAR(200),                    -- 地址
    remark      VARCHAR(500),                    -- 备注
    created_by  VARCHAR(50),                     -- 创建人
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  map_feature            IS '地图要素表';
COMMENT ON COLUMN map_feature.id         IS '主键';
COMMENT ON COLUMN map_feature.type       IS '要素类型: 1=点 2=线 3=面';
COMMENT ON COLUMN map_feature.geometry   IS 'WKT格式几何，如 POINT(116.3 39.9)';
COMMENT ON COLUMN map_feature.name       IS '名称';
COMMENT ON COLUMN map_feature.xzqh      IS '行政区划';
COMMENT ON COLUMN map_feature.address   IS '地址';
COMMENT ON COLUMN map_feature.remark    IS '备注';
COMMENT ON COLUMN map_feature.created_by IS '创建人用户名';
