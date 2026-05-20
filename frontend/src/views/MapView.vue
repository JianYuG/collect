<template>
  <div class="map-wrapper">
    <!-- 地图容器 -->
    <div ref="mapContainer" class="map-container"></div>

    <!-- 左上角工具栏：行政区划选择 + 绘制工具 -->
    <div class="top-toolbar">
      <XzqhSelector @locate="onXzqhLocate" />
      <DrawToolbar ref="drawToolbarRef" :map="map" @draw-end="onDrawEnd" />
    </div>

    <!-- 右下角：地图控制按钮（放大/缩小/图层）+ 底图切换 -->
    <div class="right-controls">
      <!-- 放大缩小与图层控制 -->
      <MapControls ref="mapControlsRef" :map="map" @feature-click="onFeatureClick" @feature-close="featureDetail.visible = false; _detailCoord = null" />

      <!-- 底图切换：只显示一个按鈕，展示当前未激活的底图缩略图，点击即切换 -->
      <div class="layer-switcher">
        <!-- 当前为电子图，显示影像预览，点击切换到影像 -->
        <div
          v-if="currentBaseLayer === 'vec'"
          class="layer-item"
          title="切换到影像地图"
          @click="switchBaseLayer('img')"
        >
          <img :src="imgLayerImg" alt="影像地图" />
          <span class="layer-tag">影像</span>
        </div>
        <!-- 当前为影像图，显示电子预览，点击切换到电子 -->
        <div
          v-else
          class="layer-item"
          title="切换到电子地图"
          @click="switchBaseLayer('vec')"
        >
          <img :src="vecLayerImg" alt="电子地图" />
          <span class="layer-tag">电子</span>
        </div>
      </div>
    </div>

    <!-- 要素详情面板（跟随点击位置） -->
    <transition name="detail-fade">
      <div
        v-if="featureDetail.visible"
        class="feature-detail-popup"
        :style="{ left: featureDetail.x + 'px', top: featureDetail.y + 'px' }"
      >
        <div class="fdp-header">
          <span class="fdp-title">要素详情</span>
          <button class="fdp-close" @click="featureDetail.visible = false; _detailCoord = null">×</button>
        </div>
        <div class="fdp-body">
          <div class="fdp-row">
            <span class="fdp-label">名称</span>
            <span class="fdp-value">{{ featureDetail.data.name || '—' }}</span>
          </div>
          <div class="fdp-row">
            <span class="fdp-label">类型</span>
            <span class="fdp-value">{{ featureTypeLabel(featureDetail.data.type) }}</span>
          </div>
          <div class="fdp-row">
            <span class="fdp-label">行政区划</span>
            <span class="fdp-value">{{ featureDetail.data.xzqhname || '—' }}</span>
          </div>
          <div class="fdp-row">
            <span class="fdp-label">区划代码</span>
            <span class="fdp-value">{{ featureDetail.data.code || '—' }}</span>
          </div>
          <div class="fdp-row">
            <span class="fdp-label">地址</span>
            <span class="fdp-value">{{ featureDetail.data.address || '—' }}</span>
          </div>
          <div class="fdp-row">
            <span class="fdp-label">备注</span>
            <span class="fdp-value">{{ featureDetail.data.remark || '—' }}</span>
          </div>
          <div class="fdp-row">
            <span class="fdp-label">创建人</span>
            <span class="fdp-value">{{ featureDetail.data.createdBy || '—' }}</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- 属性填写弹窗 -->
    <FeatureFormDialog
      v-model="dialogVisible"
      :draw-type="pendingFeature.type"
      @submit="onFormSubmit"
      @cancel="onFormCancel"
    />
  </div>
</template>

<script setup>
import { ref, reactive, shallowRef, onMounted, onBeforeUnmount } from 'vue'
import { markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import Map from 'ol/Map'
import View from 'ol/View'
import { transformExtent } from 'ol/proj'
import {
  createVecLayer,
  createCvaLayer,
  createImgLayer,
  createCiaLayer,
  getProjection4490
} from '../utils/tdtLayers'
import DrawToolbar from '../components/DrawToolbar.vue'
import FeatureFormDialog from '../components/FeatureFormDialog.vue'
import XzqhSelector from '../components/XzqhSelector.vue'
import MapControls from '../components/MapControls.vue'
import { createFeature } from '../api/feature'

// 导入图层切换图标
import vecLayerImg from '../assets/images/vec-layer.svg'
import imgLayerImg from '../assets/images/img-layer.svg'

const mapContainer = ref(null)
// shallowRef 避免 Vue 对 OL Map 对象做深度代理（深代理会破坏 OL 内部方法）
const map = shallowRef(null)
const drawToolbarRef = ref(null)
const mapControlsRef = ref(null)

// 图层实例
let vecLayer = null
let cvaLayer = null
let imgLayer = null
let ciaLayer = null

// 当前底图类型
const currentBaseLayer = ref('vec')

// 弹窗状态
const dialogVisible = ref(false)
const pendingFeature = reactive({ type: 0, geometry: '' })

// 要素详情面板状态
const featureDetail = reactive({ visible: false, x: 0, y: 0, data: {} })
// 记录点击时的地理坐标（地图移动时重新计算像素位置）
let _detailCoord = null

function featureTypeLabel(type) {
  return { 1: '点要素', 2: '线要素', 3: '面要素' }[type] || '未知'
}

/**
 * 将地理坐标转成像素坐标，计算面板安全位置
 */
function calcPanelPos(coord) {
  if (!map.value || !coord) return
  const pixel = map.value.getPixelFromCoordinate(coord)
  if (!pixel) return
  const PANEL_W = 230
  const PANEL_H = 240
  const mapEl = mapContainer.value
  const mapW = mapEl ? mapEl.clientWidth : window.innerWidth
  const mapH = mapEl ? mapEl.clientHeight : window.innerHeight
  let x = pixel[0] + 12
  let y = pixel[1] + 12
  if (x + PANEL_W > mapW) x = pixel[0] - PANEL_W - 12
  if (y + PANEL_H > mapH) y = pixel[1] - PANEL_H - 12
  if (x < 0) x = 8
  if (y < 0) y = 8
  featureDetail.x = x
  featureDetail.y = y
}

/**
 * 要素点击回调：记录地理坐标，跟随定位详情面板
 */
function onFeatureClick({ data, pixel }) {
  // 像素 → 地理坐标，供地图移动时重算
  _detailCoord = map.value ? map.value.getCoordinateFromPixel(pixel) : null
  featureDetail.data = data
  featureDetail.visible = true
  calcPanelPos(_detailCoord)
}

/**
 * 初始化地图
 */
function initMap() {
  const projection = getProjection4490()

  vecLayer = createVecLayer()
  cvaLayer = createCvaLayer()
  imgLayer = createImgLayer()
  ciaLayer = createCiaLayer()

  imgLayer.setVisible(false)
  ciaLayer.setVisible(false)

  // markRaw 防止 Vue 响应式系统代理 OL Map 对象
  map.value = markRaw(new Map({
    target: mapContainer.value,
    layers: [vecLayer, cvaLayer, imgLayer, ciaLayer],
    view: new View({
      projection: projection,
      center: [116.4, 39.9],
      zoom: 10,
      minZoom: 1,
      maxZoom: 18,
      extent: [-180, -90, 180, 90],
      constrainRotation: false
    }),
    controls: []
  }))
}

/**
 * 切换底图
 */
function switchBaseLayer(type) {
  if (currentBaseLayer.value === type) return
  currentBaseLayer.value = type

  if (type === 'vec') {
    vecLayer.setVisible(true)
    cvaLayer.setVisible(true)
    imgLayer.setVisible(false)
    ciaLayer.setVisible(false)
  } else if (type === 'img') {
    vecLayer.setVisible(false)
    cvaLayer.setVisible(false)
    imgLayer.setVisible(true)
    ciaLayer.setVisible(true)
  }
}

/**
 * 行政区划选择回调：定位到选中区域
 */
function onXzqhLocate(info) {
  if (!info || !map.value) return
  const view = map.value.getView()

  if (info.bbox) {
    // bbox 已是 EPSG:4490 经纬度，与地图投影一致，直接 fit
    const [minX, minY, maxX, maxY] = info.bbox
    view.fit([minX, minY, maxX, maxY], {
      size: map.value.getSize(),
      padding: [40, 40, 40, 40],
      duration: 600
    })
  } else if (info.centroid) {
    view.animate({
      center: info.centroid,
      zoom: 12,
      duration: 600
    })
  }
}

/**
 * 绘制完成回调：暂存几何信息，弹出属性填写弹窗
 */
function onDrawEnd({ type, geometry }) {
  pendingFeature.type = type       // 1/2/3
  pendingFeature.geometry = geometry  // WKT
  dialogVisible.value = true
}

/**
 * 属性表单提交：调用后端接口保存
 */
async function onFormSubmit(fields) {
  try {
    await createFeature({
      type: pendingFeature.type,       // 1/2/3
      geometry: pendingFeature.geometry, // WKT
      name: fields.name,
      xzqhname: fields.xzqhname,
      code: fields.code,
      address: fields.address,
      remark: fields.remark
    })
    ElMessage.success('要素保存成功')
    dialogVisible.value = false
    drawToolbarRef.value?.clearAll()  // 清除草稿图形并退出激活状态
    mapControlsRef.value?.reloadLayers()  // 重新加载地图要素
  } catch (error) {
    ElMessage.error('保存失败，请重试')
    console.error('保存要素失败:', error)
  }
}

/**
 * 取消弹窗：清除草稿图形并退出激活状态
 */
function onFormCancel() {
  drawToolbarRef.value?.clearAll()
}

onMounted(() => {
  initMap()
  // initMap 同步执行，map.value 已赋値，主动触发图层加载
  mapControlsRef.value?.initLayers()
  // 地图移动/缩放后重算详情面板位置
  map.value.on('moveend', () => {
    if (featureDetail.visible && _detailCoord) {
      calcPanelPos(_detailCoord)
    }
  })
})

onBeforeUnmount(() => {
  if (map.value) {
    map.value.setTarget(null)
    map.value = null
  }
})
</script>

<style scoped>
.map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.map-container {
  width: 100%;
  height: 100%;
}

/* 左上角工具栏：行政区划选择 + 绘制工具 */
.top-toolbar {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 右下角：控制组合区域 */
.right-controls {
  position: absolute;
  right: 16px;
  bottom: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  z-index: 10;
}

/* 底图切换 */
.layer-switcher {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.layer-item {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid rgba(255,255,255,0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
  transition: box-shadow 0.2s, transform 0.15s;
  background: #fff;
}

.layer-item:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.45);
  transform: scale(1.05);
}

.layer-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 图层标签（显示在图片上方） */
.layer-tag {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 10px;
  color: #fff;
  background: rgba(0,0,0,0.45);
  padding: 2px 0;
  letter-spacing: 0.5px;
}

/* 要素详情面板（跟随点击位置） */
.feature-detail-popup {
  position: absolute;
  z-index: 100;
  width: 230px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.22);
  overflow: hidden;
  pointer-events: auto;
}

.fdp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.fdp-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.fdp-close {
  border: none;
  background: none;
  font-size: 16px;
  color: #909399;
  cursor: pointer;
  line-height: 1;
  padding: 0 2px;
}

.fdp-close:hover { color: #303133; }

.fdp-body {
  padding: 4px 0;
}

.fdp-row {
  display: flex;
  align-items: flex-start;
  padding: 6px 12px;
  border-bottom: 1px solid #f0f2f5;
  gap: 8px;
}

.fdp-row:last-child { border-bottom: none; }

.fdp-label {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  width: 56px;
  padding-top: 1px;
}

.fdp-value {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
  flex: 1;
}

/* 详情面板出现动画 */
.detail-fade-enter-active,
.detail-fade-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}
.detail-fade-enter-from,
.detail-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
