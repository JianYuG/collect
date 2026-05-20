<template>
  <div class="map-controls">
    <!-- 放大按钮 -->
    <button class="ctrl-btn" title="放大" @click="zoomIn">
      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
        <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd"/>
      </svg>
    </button>

    <!-- 缩小按钮 -->
    <button class="ctrl-btn" title="缩小" @click="zoomOut">
      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
        <path fill-rule="evenodd" d="M3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd"/>
      </svg>
    </button>

    <!-- 查询按钮 -->
    <button
      class="ctrl-btn"
      :class="{ active: queryMode }"
      title="点击查询（开启/关闭）"
      @click="toggleQueryMode"
    >
      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
        <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
      </svg>
    </button>

    <!-- 图层控制按钮 -->
    <button
      class="ctrl-btn"
      :class="{ active: layerPanelVisible }"
      title="图层控制"
      @click="toggleLayerPanel"
    >
      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
        <path d="M3 4a1 1 0 000 2h14a1 1 0 100-2H3zm0 5a1 1 0 000 2h14a1 1 0 100-2H3zm0 5a1 1 0 000 2h14a1 1 0 100-2H3z"/>
      </svg>
    </button>

    <!-- 图层控制面板 -->
    <transition name="panel-slide">
      <div v-if="layerPanelVisible" class="layer-panel">
        <div class="panel-header">
          <span class="panel-title">图层控制</span>
          <button class="close-btn" @click="layerPanelVisible = false">×</button>
        </div>
        <div class="panel-body">
          <div v-if="loading" class="panel-loading">加载中...</div>
          <div v-else-if="layers.length === 0" class="panel-empty">暂无要素数据</div>
          <template v-else>
            <div
              v-for="layer in layers"
              :key="layer.type"
              class="layer-item"
            >
              <label class="layer-label">
                <input
                  type="checkbox"
                  :checked="layer.visible"
                  @change="toggleLayer(layer)"
                />
                <span
                  class="layer-icon"
                  :style="{ background: layer.color }"
                ></span>
                <span class="layer-name">{{ layer.label }}</span>
                <span class="layer-count">{{ layer.features.length }}</span>
              </label>
            </div>
          </template>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, watch, defineExpose, markRaw, onBeforeUnmount } from 'vue'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import Feature from 'ol/Feature'
import WKT from 'ol/format/WKT'
import { Style, Fill, Stroke, Circle as CircleStyle } from 'ol/style'
import { getAllFeatures } from '../api/feature'

// ★ OL 对象放在 setup 之外（模块级），完全脱离 Vue 响应式追踪
const _wktFormat = markRaw(new WKT())
const _olLayers = {} // type → VectorLayer 映射

// 模块级要素属性缓存（id → featureData）
const _featureMap = {}

const props = defineProps({
  map: { type: Object, default: null }
})

const emit = defineEmits(['feature-click', 'feature-close'])

// 图层面板显示状态
const layerPanelVisible = ref(false)
const loading = ref(false)

// 查询模式状态
const queryMode = ref(false)

// 图层配置：type 1=点 2=线 3=面
const LAYER_CONFIG = [
  { type: 1, label: '点要素', color: '#f56c6c' },
  { type: 2, label: '线要素', color: '#409eff' },
  { type: 3, label: '面要素', color: '#67c23a' }
]

// 图层状态列表（仅存轻量 UI 数据）
const layers = ref([])

// 类型标签
function typeLabel(type) {
  return { 1: '点要素', 2: '线要素', 3: '面要素' }[type] || '未知'
}

function makeStyle(type, color) {
  if (type === 1) {
    return new Style({
      image: new CircleStyle({
        radius: 6,
        fill: new Fill({ color }),
        stroke: new Stroke({ color: '#fff', width: 1.5 })
      })
    })
  } else if (type === 2) {
    return new Style({
      stroke: new Stroke({ color, width: 2.5 })
    })
  } else {
    return new Style({
      fill: new Fill({ color: color + '55' }),
      stroke: new Stroke({ color, width: 2 })
    })
  }
}

/**
 * 初始化图层并加载数据
 */
async function initLayers() {
  if (!props.map) return
  // 防止重复初始化
  if (layers.value.length > 0) return
  await loadLayers()
}

/**
 * 强制重新加载图层（保存新要素后调用）
 */
async function reloadLayers() {
  if (!props.map) return
  // 将旧图层从地图移除
  Object.keys(_olLayers).forEach(type => {
    props.map.removeLayer(_olLayers[type])
    delete _olLayers[type]
  })
  // 清空要素缓存
  Object.keys(_featureMap).forEach(k => delete _featureMap[k])
  // 重置 UI 状态
  layers.value = []
  await loadLayers()
}

/**
 * 内部：实际加载逻辑
 */
async function loadLayers() {
  loading.value = true
  try {
    const res = await getAllFeatures()
    const allFeatures = res.data || []

    layers.value = LAYER_CONFIG.map(cfg => {
      const features = allFeatures.filter(f => f.type === cfg.type)

      // ★ 用模块级 _wktFormat 解析，全程 markRaw 保护
      const olFeatures = features
        .map(f => {
          try {
            const geom = markRaw(_wktFormat.readGeometry(f.geometry))
            const olFeature = markRaw(new Feature({ geometry: geom }))
            olFeature.setId(f.id)
            // 缓存要素属性
            _featureMap[f.id] = f
            return olFeature
          } catch (e) {
            console.warn('WKT 解析失败:', f.geometry, e)
            return null
          }
        })
        .filter(Boolean)

      // 创建矢量图层
      const source = markRaw(new VectorSource({ features: olFeatures }))
      const layer = markRaw(new VectorLayer({
        source,
        style: makeStyle(cfg.type, cfg.color),
        zIndex: 10 + cfg.type
      }))
      props.map.addLayer(layer)
      _olLayers[cfg.type] = layer

      return {
        type: cfg.type,
        label: cfg.label,
        color: cfg.color,
        visible: true,
        features
      }
    })
  } catch (e) {
    console.error('加载要素图层失败:', e)
  } finally {
    loading.value = false
  }
}

/**
 * 切换图层显示/隐藏
 */
function toggleLayer(layer) {
  layer.visible = !layer.visible
  const olLayer = _olLayers[layer.type]
  if (olLayer) olLayer.setVisible(layer.visible)
}

/**
 * 地图点击回调
 */
let _clickHandler = null

function onMapClick(evt) {
  if (!props.map) return
  const hit = props.map.forEachFeatureAtPixel(
    evt.pixel,
    (feature) => feature,
    { hitTolerance: 6 }
  )
  if (hit) {
    const id = hit.getId()
    const data = _featureMap[id]
    if (data) {
      // 将像素坐标传给父组件定位弹框
      emit('feature-click', { data, pixel: evt.pixel })
    }
  } else {
    emit('feature-close')
  }
}

/**
 * 开启/关闭点击查询模式
 */
function toggleQueryMode() {
  queryMode.value = !queryMode.value
  if (!props.map) return
  if (queryMode.value) {
    _clickHandler = onMapClick
    props.map.on('click', _clickHandler)
    // 开启查询时鼠标变十字
    props.map.getTargetElement().style.cursor = 'pointer'
  } else {
    if (_clickHandler) {
      props.map.un('click', _clickHandler)
      _clickHandler = null
    }
    emit('feature-close')
    props.map.getTargetElement().style.cursor = ''
  }
}
function toggleLayerPanel() {
  layerPanelVisible.value = !layerPanelVisible.value
}

/**
 * 放大
 */
function zoomIn() {
  if (!props.map) return
  const view = props.map.getView()
  view.animate({ zoom: view.getZoom() + 1, duration: 250 })
}

/**
 * 缩小
 */
function zoomOut() {
  if (!props.map) return
  const view = props.map.getView()
  view.animate({ zoom: view.getZoom() - 1, duration: 250 })
}

// 地图实例就绪时自动加载所有要素图层
watch(() => props.map, (val) => {
  if (val && layers.value.length === 0) {
    initLayers()
  }
}, { immediate: true })

// 组件销毁时清理地图点击监听
onBeforeUnmount(() => {
  if (_clickHandler && props.map) {
    props.map.un('click', _clickHandler)
    _clickHandler = null
  }
})

// 暴露 initLayers/reloadLayers 供父组件调用
defineExpose({ initLayers, reloadLayers })
</script>

<style scoped>
.map-controls {
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
}

/* 控制按钮 */
.ctrl-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 6px;
  background: #fff;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: background 0.15s, color 0.15s, box-shadow 0.15s;
}

.ctrl-btn:hover {
  background: #ecf5ff;
  color: #409eff;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.25);
}

.ctrl-btn.active {
  background: #409eff;
  color: #fff;
  box-shadow: 0 3px 10px rgba(64, 158, 255, 0.4);
}

/* 图层面板 */
.layer-panel {
  position: absolute;
  right: 44px;
  bottom: 0;
  width: 190px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  z-index: 20;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.close-btn {
  border: none;
  background: none;
  font-size: 16px;
  color: #909399;
  cursor: pointer;
  line-height: 1;
  padding: 0 2px;
}

.close-btn:hover {
  color: #303133;
}

.panel-body {
  padding: 8px 0;
}

.panel-loading,
.panel-empty {
  padding: 12px 14px;
  font-size: 13px;
  color: #909399;
  text-align: center;
}

/* 图层条目 */
.layer-item {
  padding: 0;
}

.layer-label {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.layer-label:hover {
  background: #f5f7fa;
}

.layer-label input[type='checkbox'] {
  width: 14px;
  height: 14px;
  cursor: pointer;
  flex-shrink: 0;
  accent-color: #409eff;
}

.layer-icon {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  flex-shrink: 0;
}

.layer-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
}

.layer-count {
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  padding: 1px 6px;
  border-radius: 10px;
}

/* 面板滑入动画 */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* 详情面板 */
.detail-panel {
  position: absolute;
  right: 44px;
  bottom: 80px;
  width: 220px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  z-index: 20;
}

.detail-body {
  padding: 4px 0;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  padding: 6px 14px;
  border-bottom: 1px solid #f0f2f5;
  gap: 8px;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  width: 56px;
  padding-top: 1px;
}

.detail-value {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
  flex: 1;
}
</style>
