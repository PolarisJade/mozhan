<template>
  <PageShell
    title="评论管理"
    :page="page"
    :loading="loading"
    @size-change="onSizeChange"
    @current-change="onCurrentChange"
  >
    <!--
      两级用树形行展开来呈现，而不是把回复拍平成同样缩进的行：
      拍平后翻页会错位（一条一级评论跨页），也没法一眼看出哪几条属于同一条评论。
    -->
    <el-table
      :data="tableData"
      border
      row-key="id"
      :tree-props="{ children: 'replies' }"
      style="width: 100%"
    >
      <el-table-column label="序号" width="60">
        <template #default="scope">
          <span v-if="!scope.row.parentId">{{ scope.$index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评论人" width="140">
        <template #default="scope">
          <span :class="{ 'reply-indent': !!scope.row.parentId }">
            {{ scope.row.nickname || '—' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="评论内容" min-width="320">
        <template #default="scope">
          <!-- 回复行标出被回复的人，否则同一串回复读不出谁在跟谁说话 -->
          <span v-if="scope.row.parentId && scope.row.replyToNickname" class="reply-to">
            回复 @{{ scope.row.replyToNickname }}：
          </span>
          <span>{{ scope.row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="articleTitle" label="评论文章" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.articleTitle || '（文章已删除）' }}
        </template>
      </el-table-column>
      <el-table-column label="回复数" width="90">
        <template #default="scope">
          <span v-if="!scope.row.parentId && scope.row.totalReplies">
            {{ scope.row.totalReplies }}
          </span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="scope">
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </PageShell>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import PageShell from '@/components/PageShell.vue'
import { useTable } from '@/composables/useTable'
import { getCommentPage, deleteComment } from '@/api/admin'

const { tableData, loading, page, load, onSizeChange, onCurrentChange } = useTable(getCommentPage, {
  errorMessage: '获取评论列表失败',
  // replies 为空时置为 undefined：Element Plus 靠 children 是否存在决定要不要画展开箭头，
  // 留一个空数组会让每条没有回复的评论都长出一个点了没反应的箭头。
  transform: row => ({
    ...row,
    replies: row.replies && row.replies.length ? row.replies : undefined
  })
})

async function handleDelete(row) {
  const isRoot = !row.parentId
  const extra = isRoot && row.totalReplies
    ? `该评论下有 ${row.totalReplies} 条回复，会一并从列表中消失。`
    : ''
  try {
    await ElMessageBox.confirm(`确定删除该评论吗？${extra}`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteComment(row.id)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && page.num > 1) page.num -= 1
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}
</script>

<style scoped>
.reply-indent {
  padding-left: 4px;
  color: var(--ink-light, #4a4a4a);
}

.reply-to {
  color: var(--ink-muted, #8a8580);
}
</style>
