<template>
  <el-dialog
    v-model="visible"
    :title="`新增${typeLabel}要素`"
    width="480px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
    >
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入名称" clearable />
      </el-form-item>
      <el-form-item label="行政区划">
        <el-cascader
          v-model="xzqhSelected"
          :options="xzqhTree"
          :props="cascaderProps"
          :loading="xzqhLoading"
          clearable
          filterable
          placeholder="请选择行政区划（可搜索）"
          style="width: 100%"
          @change="onXzqhChange"
        />
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model="form.address" placeholder="请输入详细地址" clearable />
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="备注信息（选填）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取 消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        {{ loading ? '保存中...' : '保 存' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getXzqhTree } from '../api/xzqh'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  drawType: { type: Number, default: 1 }  // 1=点 2=线 3=面
})

const emit = defineEmits(['update:modelValue', 'submit', 'cancel'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const typeLabel = computed(() => ({
  1: '点',
  2: '线',
  3: '面'
}[props.drawType] || ''))

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  name: '',
  address: '',
  remark: ''
})

// 行政区划级联选择器状态
const xzqhSelected = ref(null)   // 选中的 code
const xzqhName = ref('')          // 选中的名称（用于保存）
const xzqhCode = ref('')          // 选中的代码（用于保存）
const xzqhTree = ref([])
const xzqhLoading = ref(false)

const cascaderProps = {
  checkStrictly: true,  // 允许选中任意一级
  emitPath: false       // 只返回最后一级 value(code)
}

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

// 加载行政区划树
onMounted(async () => {
  xzqhLoading.value = true
  try {
    const res = await getXzqhTree()
    xzqhTree.value = res.data || []
  } catch (e) {
    ElMessage.error('行政区划数据加载失败')
  } finally {
    xzqhLoading.value = false
  }
})

// 弹窗打开时重置表单
watch(() => props.modelValue, (val) => {
  if (val) resetForm()
})

function resetForm() {
  form.name = ''
  form.address = ''
  form.remark = ''
  xzqhSelected.value = null
  xzqhName.value = ''
  xzqhCode.value = ''
  formRef.value?.clearValidate()
}

/** 行政区划选中时，记录 name 和 code */
function onXzqhChange(code) {
  if (!code) {
    xzqhName.value = ''
    xzqhCode.value = ''
    return
  }
  const node = findNode(xzqhTree.value, code)
  if (node) {
    xzqhName.value = node.fullname || node.label
    xzqhCode.value = node.code || node.value
  }
}

function findNode(nodes, code) {
  for (const n of nodes) {
    if (n.value === code) return n
    if (n.children && n.children.length) {
      const found = findNode(n.children, code)
      if (found) return found
    }
  }
  return null
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    emit('submit', {
      name: form.name,
      xzqhname: xzqhName.value,
      code: xzqhCode.value,
      address: form.address,
      remark: form.remark
    })
  } finally {
    loading.value = false
  }
}

function handleClose() {
  emit('cancel')
  emit('update:modelValue', false)
}

// 暴露 setLoading 让父组件控制加载态
defineExpose({
  setLoading: (val) => { loading.value = val }
})
</script>
