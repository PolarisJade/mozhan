<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>评论管理</h2>
    </div>
    <div class="table-container">
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="content" label="内容" min-width="300" />
        <el-table-column prop="nickname" label="评论者" width="120" />
        <el-table-column prop="articleTitle" label="文章标题" width="150" />
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
import { getCommentPage, deleteComment } from '@/api/admin'

const tableData = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

async function fetchData() {
  try {
    const res = await getCommentPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('获取评论列表失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除该评论吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteComment(row.id)
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

.table-container {
  margin-bottom: 24px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
}
</style>
