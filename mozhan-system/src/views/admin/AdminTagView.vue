<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>标签管理</h2>
      <el-button type="primary" @click="openAddDialog">添加标签</el-button>
    </div>
    <div class="table-container">
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" />
        <el-table-column prop="createBy" label="创建人" width="120" />
        <el-table-column prop="articleCount" label="文章数" width="100" />
        <el-table-column prop="essayCount" label="随笔数" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination-container">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.pageNum"
        :page-sizes="[10, 20, 50]"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <el-dialog title="添加标签" v-model="dialogVisible" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTagPage, addTag, deleteTag } from '@/api/admin'

const tableData = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  name: ''
})

const rules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { max: 20, message: '标签名称不能超过20个字符', trigger: 'blur' }
  ]
}

async function fetchData() {
  try {
    const res = await getTagPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('获取标签列表失败')
  }
}

function openAddDialog() {
  form.name = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await addTag({ name: form.name })
      ElMessage.success('添加成功')
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除标签「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTag(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleSizeChange(val) {
  pagination.pageSize = val
  pagination.pageNum = 1
  fetchData()
}

function handleCurrentChange(val) {
  pagination.pageNum = val
  fetchData()
}

fetchData()
</script>

<style scoped>
.admin-page {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.table-container {
  margin-bottom: 24px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
}
</style>
