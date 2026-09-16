<template>
  <PageShell
    title="用户管理"
    :page="page"
    :loading="loading"
    @size-change="onSizeChange"
    @current-change="onCurrentChange"
  >
    <template #search>
      <el-input
        v-model="searchForm.nickname"
        placeholder="搜索昵称"
        class="filter-input"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </template>

    <el-table :data="tableData" border style="width: 100%">
      <el-table-column label="序号" width="60" type="index" :index="indexMethod" />
      <el-table-column label="昵称" min-width="150">
        <template #default="scope">
          {{ scope.row.nickname }}
          <el-tag v-if="scope.row.admin" size="small" type="warning" class="admin-tag">管理员</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="账号" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column prop="articleCount" label="文章总数" width="100" />
      <el-table-column prop="essayCount" label="随笔总数" width="100" />
      <el-table-column label="账号状态" width="110">
        <template #default="scope">
          <!--
            开关直接改后端状态。失败时必须把开关退回原值，
            否则界面显示"已禁用"而后端还是启用，是最难查的那类不一致。

            管理员账号直接禁用开关：后端会拒绝，与其让用户点一下再弹错，
            不如在按下去之前就说清楚为什么不能按。
          -->
          <el-tooltip :disabled="!scope.row.admin" content="管理员账号不可禁用" placement="top">
            <el-switch
              v-model="scope.row.status"
              active-value="启用"
              inactive-value="禁用"
              :disabled="!!scope.row.admin"
              :loading="statusPending === scope.row.id"
              @change="handleStatusChange(scope.row)"
            />
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openDetail(scope.row)">查看</el-button>
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </PageShell>

  <el-dialog v-model="detailVisible" title="用户信息" width="520px">
    <div v-loading="detailLoading" class="detail-body">
      <div class="detail-head">
        <el-avatar :size="52" :src="detail.avatar">
          {{ (detail.nickname || '?').slice(0, 1) }}
        </el-avatar>
        <div>
          <div class="detail-nickname">
            {{ detail.nickname }}
            <el-tag v-if="detail.admin" size="small" type="warning" class="admin-tag">管理员</el-tag>
          </div>
          <div class="detail-sub">{{ detail.username }}</div>
        </div>
      </div>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账号">{{ detail.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname || '—' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="简介">{{ detail.intro || '—' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">{{ detail.status || '—' }}</el-descriptions-item>
        <el-descriptions-item label="文章总数">{{ detail.articleCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="随笔总数">{{ detail.essayCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ detail.createTime || '—' }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </el-dialog>

  <el-dialog v-model="editVisible" title="编辑用户" width="480px">
    <el-form :model="editForm" label-width="70px" class="ink-form">
      <el-form-item label="昵称">
        <el-input v-model="editForm.nickname" maxlength="20" show-word-limit />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="editForm.email" />
      </el-form-item>
      <el-form-item label="头像">
        <el-input v-model="editForm.avatar" placeholder="图片地址" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="editForm.intro" type="textarea" :rows="3" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <!-- 编辑项里没有密码：管理员不提供改他人密码的能力，这是刻意的 -->
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PageShell from '@/components/PageShell.vue'
import { useTable } from '@/composables/useTable'
import { getUserPage, getUserDetail, updateUser, changeUserStatus } from '@/api/admin'

const searchForm = reactive({ nickname: '' })

const { tableData, loading, page, load, search, reset, onSizeChange, onCurrentChange } = useTable(
  getUserPage,
  {
    // 只有这个接口的页码参数叫 current，其余都是 pageNum
    pageParam: 'current',
    params: () => ({ nickname: searchForm.nickname || undefined }),
    resetParams: () => { searchForm.nickname = '' },
    errorMessage: '获取用户列表失败'
  }
)

// 序号要跨页连续，不能每页都从 1 开始
function indexMethod(index) {
  return (page.num - 1) * page.size + index + 1
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({ id: null, nickname: '', email: '', avatar: '', intro: '' })

// 正在提交状态变更的行 id：用来只给那一行转圈，而不是整表
const statusPending = ref(null)

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = {}
  try {
    detail.value = await getUserDetail(row.id)
  } catch (error) {
    ElMessage.error(error.message || '获取用户信息失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function openEdit(row) {
  editForm.id = row.id
  editForm.nickname = row.nickname || ''
  editForm.email = row.email || ''
  editForm.avatar = row.avatar || ''
  editForm.intro = row.intro || ''
  editVisible.value = true
}

async function submitEdit() {
  saving.value = true
  try {
    await updateUser({ ...editForm })
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleStatusChange(row) {
  const next = row.status
  statusPending.value = row.id
  try {
    await changeUserStatus(row.id, next)
    ElMessage.success(next === '启用' ? '已启用' : '已禁用')
  } catch (error) {
    // 退回原值，并让界面与后端重新对齐
    row.status = next === '启用' ? '禁用' : '启用'
    ElMessage.error(error.message || '操作失败')
  } finally {
    statusPending.value = null
  }
}
</script>

<style scoped>
.admin-tag {
  margin-left: 6px;
}

.detail-body {
  min-height: 120px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.detail-nickname {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink, #1a1a1a);
}

.detail-sub {
  font-size: 13px;
  color: var(--ink-muted, #8a8580);
  margin-top: 2px;
}
</style>
