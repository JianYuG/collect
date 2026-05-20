-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '主键ID';
COMMENT ON COLUMN sys_user.username IS '用户名，唯一';
COMMENT ON COLUMN sys_user.password IS '密码（BCrypt加密）';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.status IS '状态：1-正常，0-禁用';
COMMENT ON COLUMN sys_user.created_at IS '创建时间';
COMMENT ON COLUMN sys_user.updated_at IS '更新时间';

-- 地图要素表
CREATE TABLE IF NOT EXISTS map_feature (
    id          BIGSERIAL    PRIMARY KEY,
    type        SMALLINT     NOT NULL,           -- 要素类型: 1=点 2=线 3=面
    geometry    TEXT         NOT NULL,           -- WKT 格式几何（PostGIS 可直接转 geometry）
    name        VARCHAR(100),                    -- 名称
    xzqhname    VARCHAR(100),                    -- 行政区划名称
    code        VARCHAR(20),                     -- 行政区划代码
    address     VARCHAR(200),                    -- 地址
    remark      VARCHAR(500),                    -- 备注
    created_by  VARCHAR(50),                     -- 创建人
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  map_feature              IS '地图要素表';
COMMENT ON COLUMN map_feature.id           IS '主键';
COMMENT ON COLUMN map_feature.type         IS '要素类型: 1=点 2=线 3=面';
COMMENT ON COLUMN map_feature.geometry     IS 'WKT格式几何，如 POINT(116.3 39.9)';
COMMENT ON COLUMN map_feature.name         IS '名称';
COMMENT ON COLUMN map_feature.xzqhname     IS '行政区划名称';
COMMENT ON COLUMN map_feature.code         IS '行政区划代码';
COMMENT ON COLUMN map_feature.address      IS '地址';
COMMENT ON COLUMN map_feature.remark       IS '备注';
COMMENT ON COLUMN map_feature.created_by   IS '创建人用户名';
