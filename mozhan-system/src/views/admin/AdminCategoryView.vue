<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="openAddDialog">添加分类</el-button>
    </div>
    <div class="table-container">
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column prop="articleCount" label="文章数" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
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
import { getCategoryPage, addCategory, updateCategory, deleteCategory } from '@/api/admin'

const tableData = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('添加分类')
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  sort: 0
})

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 20, message: '分类名称不能超过20个字符', trigger: 'blur' }
  ]
}

async function fetchData() {
  try {
    const res = await getCategoryPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('获取分类列表失败')
  }
}

function openAddDialog() {
  dialogTitle.value = '添加分类'
  form.id = null
  form.name = ''
  form.sort = 0
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogTitle.value = '编辑分类'
  form.id = row.id
  form.name = row.name
  form.sort = row.sort
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (form.id) {
        await updateCategory(form.id, { name: form.name, sort: form.sort })
        ElMessage.success('更新成功')
      } else {
        await addCategory({ name: form.name, sort: form.sort })
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCategory(row.id)
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
