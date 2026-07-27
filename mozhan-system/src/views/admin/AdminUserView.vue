<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>用户管理</h2>
    </div>
    <div class="search-bar">
      <el-input
        v-model="searchForm.nickname"
        placeholder="搜索昵称"
        class="search-input"
        clearable
        @keyup.enter="fetchData"
      />
      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>
    <div class="table-container">
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column label="序号" width="60" type="index" :index="(index) => (pagination.current - 1) * pagination.pageSize + index + 1" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="articleCount" label="文章总数" width="100" />
        <el-table-column prop="essayCount" label="随笔总数" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <span :class="scope.row.status === '启用' ? 'status-active' : 'status-inactive'">
              {{ scope.row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" @click="viewUser(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination-container">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.current"
        :page-sizes="[10, 20, 50]"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        :page-size-options="['10', '20', '50']"
        background
        prev-text="上一页"
        next-text="下一页"
        :disabled="pagination.total === 0"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserPage } from '@/api/admin'

const tableData = ref([])

const searchForm = reactive({
  nickname: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

async function fetchData() {
  try {
    const res = await getUserPage({
      current: pagination.current,
      pageSize: pagination.pageSize,
      nickname: searchForm.nickname || undefined
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  }
}

function resetSearch() {
  searchForm.nickname = ''
  pagination.current = 1
  fetchData()
}

function handleSizeChange(val) {
  pagination.pageSize = val
  pagination.current = 1
  fetchData()
}

function handleCurrentChange(val) {
  pagination.current = val
  fetchData()
}

function viewUser(row) {
  ElMessage.info(`查看用户: ${row.nickname}`)
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
