import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 分页列表的公共逻辑：拉取、翻页、每页条数、搜索、重置。
 *
 * 这段逻辑本来在六个管理页里逐字重复了六遍，各自的差别只有两处：
 * 分页参数叫什么（用户页用 current，其余用 pageNum），以及搜索条件怎么拼。
 * 所以这两点做成参数，其余收敛到这里——以后改分页行为只用改一个文件。
 *
 * @param fetcher 调用后端的函数，收到 { ...分页参数, ...搜索条件 }
 * @param options.pageParam    分页页码的参数名，默认 pageNum；用户页要传 'current'
 * @param options.params       返回当前搜索条件的函数
 * @param options.resetParams  清空搜索条件；不传则「重置」只回到第一页
 * @param options.transform    对返回的 records 逐行加工
 * @param options.errorMessage 拉取失败时的兜底提示
 */
export function useTable(fetcher, options = {}) {
  const {
    pageParam = 'pageNum',
    pageSize: initialSize = 10,
    params = () => ({}),
    resetParams,
    transform,
    errorMessage = '加载失败',
    immediate = true
  } = options

  const tableData = ref([])
  const loading = ref(false)
  const page = reactive({ num: 1, size: initialSize, total: 0 })

  async function load() {
    loading.value = true
    try {
      const res = await fetcher({
        [pageParam]: page.num,
        pageSize: page.size,
        ...params()
      })
      const records = res.records || []
      tableData.value = transform ? records.map(transform) : records
      page.total = res.total || 0
    } catch (error) {
      ElMessage.error(error?.message || errorMessage)
      // 失败时清空而不是留着上一次的数据：否则筛选条件已经变了、表里还是旧行，
      // 看起来像"搜到了"但其实是上一次的结果，是最容易误判的一种状态。
      tableData.value = []
      page.total = 0
    } finally {
      loading.value = false
    }
  }

  function search() {
    page.num = 1
    return load()
  }

  function reset() {
    if (resetParams) resetParams()
    return search()
  }

  function onSizeChange(size) {
    page.size = size
    page.num = 1
    return load()
  }

  function onCurrentChange(num) {
    page.num = num
    return load()
  }

  if (immediate) load()

  return { tableData, loading, page, load, search, reset, onSizeChange, onCurrentChange }
}
