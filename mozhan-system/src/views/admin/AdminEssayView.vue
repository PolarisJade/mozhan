<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>随笔管理</h2>
    </div>
    <div class="search-bar">
      <el-input
        v-model="searchForm.title"
        placeholder="搜索标题"
        class="search-input"
        clearable
        @keyup.enter="fetchData"
      />
      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>
    <div class="table-container">
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="nickname" label="作者" width="120" />
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <span :class="scope.row.status === 1 ? 'status-active' : 'status-inactive'">
              {{ scope.row.status === 1 ? '发布' : '草稿' }}
            </span>
          </template>
        </el-table-column>
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
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEssayPage, deleteEssay } from '@/api/admin'

const tableData = ref([])

const searchForm = reactive({
  title: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

async function fetchData() {
  try {
    const res = await getEssayPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      title: searchForm.title || undefined
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('获取随笔列表失败')
  }
}

function resetSearch() {
  searchForm.title = ''
  pagination.pageNum = 1
  fetchData()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除随笔「${row.title}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteEssay(row.id)
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
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input {
  width: 200px;
}

.table-container {
  margin-bottom: 24px;
}

.status-active {
  color: #67c23a;
}

.status-inactive {
  color: #f56c6c;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
}
</style>
