<template>
  <PageShell
    title="文章管理"
    :page="page"
    :loading="loading"
    @size-change="onSizeChange"
    @current-change="onCurrentChange"
  >
    <template #search>
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索文章标题"
        class="filter-input"
        clearable
        @keyup.enter="search"
      />
      <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable class="filter-select">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="searchForm.tagId" placeholder="全部标签" clearable filterable class="filter-select">
        <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <el-select v-model="searchForm.status" placeholder="全部状态" clearable class="filter-select">
        <!--
          这里传的是枚举名（PUBLISHED/DRAFT），不是列表里显示的「发布/草稿」。
          列表值是 Jackson 用 @JsonValue 给的中文，查询参数走的是另一条转换链。
          后端为了防这个坑两种写法都收，但这里仍按枚举名传，语义更明确。
        -->
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </template>

    <el-table :data="tableData" border style="width: 100%">
      <el-table-column prop="authorName" label="作者" width="100" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="summary" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column label="标签" min-width="160">
        <template #default="scope">
          <el-tag v-for="t in scope.row.tags" :key="t.id" size="small" class="tag-item">
            {{ t.name }}
          </el-tag>
          <span v-if="!scope.row.tags || !scope.row.tags.length" class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="likeCount" label="点赞" width="80" />
      <el-table-column prop="commentCount" label="评论" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <span :class="scope.row.status === '发布' ? 'status-published' : 'status-draft'">
            {{ scope.row.status }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="修改时间" width="170" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openContent(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </PageShell>

  <el-dialog v-model="contentVisible" title="文章内容" width="820px" top="6vh">
    <div v-loading="contentLoading" class="content-dialog">
      <h3 class="content-title">{{ content.title }}</h3>
      <div class="content-meta">
        <span>{{ content.authorName }}</span>
        <span v-if="content.categoryName">· {{ content.categoryName }}</span>
        <span>· {{ content.status }}</span>
        <span>· 点赞 {{ content.likeCount }} / 评论 {{ content.commentCount }}</span>
        <span>· 创建于 {{ content.createTime }}</span>
      </div>
      <!--
        正文是后端已渲染好的 HTML（写文章页提交的就是 HTML），这里直接插入，
        不要再套一层 markdown 解析——只有「大纲」那条链路返回的才是 Markdown。
      -->
      <div class="content-body" v-html="content.content"></div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PageShell from '@/components/PageShell.vue'
import { useTable } from '@/composables/useTable'
import { getArticlePage, getArticleContent, getCategoryPage, getTagPage } from '@/api/admin'

const categories = ref([])
const tags = ref([])

const searchForm = reactive({
  keyword: '',
  categoryId: null,
  tagId: null,
  status: null
})

const { tableData, loading, page, search, reset, onSizeChange, onCurrentChange } = useTable(
  getArticlePage,
  {
    params: () => ({
      keyword: searchForm.keyword || undefined,
      categoryId: searchForm.categoryId || undefined,
      tagId: searchForm.tagId || undefined,
      status: searchForm.status || undefined
    }),
    resetParams: () => {
      searchForm.keyword = ''
      searchForm.categoryId = null
      searchForm.tagId = null
      searchForm.status = null
    },
    errorMessage: '获取文章列表失败'
  }
)

// 筛选下拉的选项量很小，一次拉满即可（分类 9 条、标签 14 条）
async function fetchFilters() {
  try {
    const [c, t] = await Promise.all([
      getCategoryPage({ pageNum: 1, pageSize: 100 }),
      getTagPage({ pageNum: 1, pageSize: 100 })
    ])
    categories.value = c.records || []
    tags.value = t.records || []
  } catch (error) {
    // 下拉拉不到不该挡住主列表，静默降级成"只能按关键词搜"
  }
}

const contentVisible = ref(false)
const contentLoading = ref(false)
const content = ref({})

async function openContent(row) {
  contentVisible.value = true
  contentLoading.value = true
  content.value = {}
  try {
    content.value = await getArticleContent(row.id)
  } catch (error) {
    ElMessage.error(error.message || '获取文章内容失败')
    contentVisible.value = false
  } finally {
    contentLoading.value = false
  }
}

fetchFilters()
</script>

<style scoped>
.tag-item {
  margin-right: 4px;
  margin-bottom: 2px;
}

.muted {
  color: var(--ink-faint, #b5b0a8);
}

.status-published {
  color: var(--el-color-success, #4a7c3f);
}

.status-draft {
  color: var(--ink-muted, #8a8580);
}

.content-dialog {
  min-height: 120px;
}

.content-title {
  margin: 0 0 8px;
  font-size: 20px;
  color: var(--ink, #1a1a1a);
}

.content-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--ink-muted, #8a8580);
  padding-bottom: 14px;
  border-bottom: 1px solid var(--paper-deep, #ebe6dc);
  margin-bottom: 16px;
}

.content-body {
  max-height: 58vh;
  overflow-y: auto;
  line-height: 1.8;
  color: var(--ink, #1a1a1a);
  word-break: break-word;
}

.content-body :deep(img) {
  max-width: 100%;
  height: auto;
}

.content-body :deep(pre) {
  background: var(--paper, #f6f3ed);
  padding: 12px;
  border-radius: 2px;
  overflow-x: auto;
}
</style>
