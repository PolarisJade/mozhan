<template>
  <section class="page-shell">
    <header class="shell-head">
      <h2 class="shell-title">{{ title }}</h2>
      <div v-if="$slots.actions" class="shell-actions">
        <slot name="actions" />
      </div>
    </header>

    <div v-if="$slots.search" class="shell-search">
      <slot name="search" />
    </div>

    <div v-loading="loading" class="shell-body">
      <slot />
    </div>

    <footer v-if="page" class="shell-foot">
      <el-pagination
        :current-page="page.num"
        :page-size="page.size"
        :page-sizes="[10, 20, 50]"
        :total="page.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="emit('size-change', $event)"
        @current-change="emit('current-change', $event)"
      />
    </footer>
  </section>
</template>

<script setup>
/**
 * 管理页的外壳：标题、右上角操作、搜索条、表格区、分页。
 *
 * 六个页面原来是各自写一遍这套结构外加约 40 行一模一样的 <style scoped>，
 * 微调一次要改六处、必然漂移。收敛到这里后，各页只管自己的列和搜索控件。
 *
 * 分页对象由 useTable 提供，这里只负责把事件转发出去，不自己维护状态。
 */
defineProps({
  title: { type: String, required: true },
  page: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['size-change', 'current-change'])
</script>

<style scoped>
.page-shell {
  background: var(--paper-solid, #fffcf7);
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 4px;
  padding: var(--sp-5, 24px);
  box-shadow: 0 8px 32px rgba(26, 26, 26, 0.05);
}

.shell-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--sp-4, 16px);
  flex-wrap: wrap;
}

.shell-title {
  margin: 0 0 var(--sp-5, 24px);
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 400;
  color: var(--ink, #1a1a1a);
  /* 汉字是等宽方块，标题拉开字距比收紧好看，和拉丁文相反 */
  letter-spacing: 0.12em;
  position: relative;
  padding-bottom: var(--sp-3, 12px);
}

/* 标题下那道由深到无的短横，是整套主题的记号 */
.shell-title::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, var(--ink, #1a1a1a), transparent);
  transition: width var(--dur-slow, 320ms) var(--ease-out);
}

.shell-actions {
  display: flex;
  gap: var(--sp-2, 8px);
  align-items: center;
}

.shell-search {
  display: flex;
  gap: var(--sp-3, 12px);
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: var(--sp-5, 24px);
}

.shell-body {
  min-height: 120px;
}

.shell-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--sp-5, 24px);
}

@media (max-width: 720px) {
  .page-shell {
    padding: var(--sp-4, 16px);
  }

  .shell-search {
    gap: var(--sp-2, 8px);
  }
}
</style>
