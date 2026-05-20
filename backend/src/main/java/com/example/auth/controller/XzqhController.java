package com.example.auth.controller;

import com.example.auth.entity.XzqhItem;
import com.example.auth.exception.Result;
import com.example.auth.mapper.XzqhMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/xzqh")
public class XzqhController {

    @Autowired
    private XzqhMapper xzqhMapper;

    /**
     * 获取行政区划树（1-4级，省/市/区县/乡镇）
     * 返回扁平列表，前端自行构建级联数据
     * 每项包含: id, name, fullname, code, level, bbox, centroid
     */
    @GetMapping("/list")
    public Result<List<XzqhItem>> list() {
        List<XzqhItem> items = xzqhMapper.listLevels1to4();
        log.info("查询行政区划列表, 共 {} 条", items.size());
        return Result.success(items);
    }

    /**
     * 获取行政区划级联树（按层级嵌套）
     * 返回结构适配 Element Plus Cascader 组件
     */
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        List<XzqhItem> all = xzqhMapper.listLevels1to4();

        // 用 code 建立 map，便于找父节点
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (XzqhItem item : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", item.getId());
            node.put("value", item.getCode());
            node.put("label", item.getName());
            node.put("fullname", item.getFullname());
            node.put("code", item.getCode());
            node.put("level", item.getLevel());
            node.put("bbox", item.getBbox());
            node.put("centroid", item.getCentroid());
            node.put("children", new ArrayList<>());

            nodeMap.put(item.getCode(), node);

            if (item.getLevel() == 1) {
                roots.add(node);
            } else {
                // 父 code：截取上一级 code
                String parentCode = getParentCode(item.getCode(), item.getLevel());
                Map<String, Object> parent = nodeMap.get(parentCode);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                }
            }
        }

        log.info("构建行政区划树完成, 根节点数: {}", roots.size());
        return Result.success(roots);
    }

    /**
     * 根据当前节点 code 和 level 推算父节点 code
     * level=2(市) -> 父=省(2位): code.substring(0,2)
     * level=3(区县) -> 父=市(4位): code.substring(0,4)
     * level=4(乡镇) -> 父=区县(6位): code.substring(0,6)
     */
    private String getParentCode(String code, int level) {
        switch (level) {
            case 2: return code.substring(0, 2);
            case 3: return code.substring(0, 4);
            case 4: return code.substring(0, 6);
            default: return null;
        }
    }
}
