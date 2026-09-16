<template>
  <PageShell
    title="分类管理"
    :page="page"
    :loading="loading"
    @size-change="onSizeChange"
    @current-change="onCurrentChange"
  >
    <template #actions>
      <el-button type="primary" @click="openAddDialog">添加分类</el-button>
    </template>

    <template #search>
      <el-input
        v-model="searchForm.name"
        placeholder="搜索分类名称"
        class="filter-input"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </template>

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
  </PageShell>

  <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" class="ink-form">
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
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageShell from '@/components/PageShell.vue'
import { useTable } from '@/composables/useTable'
import { getCategoryPage, addCategory, updateCategory, deleteCategory } from '@/api/admin'

const searchForm = reactive({ name: '' })

const { tableData, loading, page, load, search, reset, onSizeChange, onCurrentChange } = useTable(
  getCategoryPage,
  {
    params: () => ({ name: searchForm.name || undefined }),
    resetParams: () => { searchForm.name = '' },
    errorMessage: '获取分类列表失败'
  }
)

const dialogVisible = ref(false)
const dialogTitle = ref('添加分类')
const formRef = ref(null)
const form = reactive({ id: null, name: '', sort: 0 })

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 20, message: '分类名称不能超过20个字符', trigger: 'blur' }
  ]
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
  await formRef.value.validate(async valid => {
    if (!valid) return
    try {
      if (form.id) {
        await updateCategory(form.id, { name: form.name, sort: form.sort })
        ElMessage.success('更新成功')
        // 编辑不改变总条数，停在当前页即可，跳回第一页反而会丢失位置
        load()
      } else {
        await addCategory({ name: form.name, sort: form.sort })
        ElMessage.success('添加成功')
        search()
      }
      dialogVisible.value = false
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
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
    if (tableData.value.length === 1 && page.num > 1) page.num -= 1
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}
</script>
