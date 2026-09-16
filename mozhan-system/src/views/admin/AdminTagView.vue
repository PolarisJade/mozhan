<template>
  <PageShell
    title="标签管理"
    :page="page"
    :loading="loading"
    @size-change="onSizeChange"
    @current-change="onCurrentChange"
  >
    <template #actions>
      <el-button type="primary" @click="openAddDialog">添加标签</el-button>
    </template>

    <template #search>
      <el-input
        v-model="searchForm.name"
        placeholder="搜索标签名称"
        class="filter-input"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </template>

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
  </PageShell>

  <el-dialog title="添加标签" v-model="dialogVisible" width="400px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" class="ink-form">
      <el-form-item label="标签名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入标签名称" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageShell from '@/components/PageShell.vue'
import { useTable } from '@/composables/useTable'
import { getTagPage, addTag, deleteTag } from '@/api/admin'

const searchForm = reactive({ name: '' })

const { tableData, loading, page, load, search, reset, onSizeChange, onCurrentChange } = useTable(
  getTagPage,
  {
    params: () => ({ name: searchForm.name || undefined }),
    resetParams: () => { searchForm.name = '' },
    errorMessage: '获取标签列表失败'
  }
)

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ name: '' })

const rules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { max: 20, message: '标签名称不能超过20个字符', trigger: 'blur' }
  ]
}

function openAddDialog() {
  form.name = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async valid => {
    if (!valid) return
    try {
      await addTag({ name: form.name })
      ElMessage.success('添加成功')
      dialogVisible.value = false
      // 新增后回到第一页：新标签不一定落在当前页，停在原页会看不到自己刚加的东西
      search()
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
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
    // 删掉当前页最后一条时要退回上一页，否则会停在一个空页上
    if (tableData.value.length === 1 && page.num > 1) page.num -= 1
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}
</script>
