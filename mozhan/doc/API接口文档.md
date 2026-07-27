# 博客网站 API 接口文档

## 文档说明

- **项目名称**: Mozhan 博客系统
- **版本**: v1.0.0
- **基础路径**: `/api`
- **数据格式**: JSON
- **编码格式**: UTF-8

---

## 通用说明

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 响应状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或token失效 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 认证说明

需要登录的接口需在请求头中携带 Token：
```
Authorization: Bearer {token}
```

---

# 一、用户模块

## 1.1 用户注册

**接口地址**: `POST /api/user/register`

**接口描述**: 用户注册新账号

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名，4-20位字母数字下划线 |
| password | String | 是 | 密码，6-20位 |
| nickname | String | 否 | 昵称，不填默认为用户名 |
| email | String | 否 | 邮箱地址 |
| avatar | String | 否 | 头像地址 |

**请求示例**:
```json
{
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三",
  "email": "zhangsan@example.com"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": null,
    "email": "zhangsan@example.com",
    "createTime": "2026-05-12 10:00:00"
  }
}
```

---

## 1.2 用户登录

**接口地址**: `POST /api/user/login`

**接口描述**: 用户登录获取Token

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**请求示例**:
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "nickname": "张三",
      "avatar": "http://example.com/avatar.jpg",
      "email": "zhangsan@example.com",
      "intro": "这个人很懒，什么都没写"
    }
  }
}
```

---

## 1.3 用户退出登录

**接口地址**: `POST /api/user/logout`

**接口描述**: 用户退出登录，使Token失效

**是否需要认证**: 是

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "退出成功",
  "data": null
}
```

---

## 1.4 获取用户信息

**接口地址**: `GET /api/user/info/{userId}`

**接口描述**: 获取当前登录用户的详细信息

**是否需要认证**: 是

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "http://example.com/avatar.jpg",
    "intro": "这个人很懒，什么都没写",
    "email": "zhangsan@example.com",
    "status": 1,
    "createTime": "2026-05-12 10:00:00",
    "updateTime": "2026-05-12 10:00:00"
  }
}
```

---

## 1.5 修改用户信息

**接口地址**: `PUT /api/user/info/update`

**接口描述**: 修改当前登录用户的个人信息

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nickname | String | 否 | 昵称 |
| avatar | String | 否 | 头像地址 |
| intro | String | 否 | 个人简介 |
| email | String | 否 | 邮箱 |

**请求示例**:
```json
{
  "nickname": "新昵称",
  "intro": "这是我的个人简介",
  "email": "newemail@example.com"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "修改成功",
  "data": null
}
```

---

## 1.6 修改密码

**接口地址**: `PUT /api/user/password`

**接口描述**: 修改当前登录用户的密码

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | String | 是 | 原密码 |
| newPassword | String | 是 | 新密码，6-20位 |

**请求示例**:
```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

---

## 1.7 获取用户主页信息

**接口地址**: `GET /api/user/profile/{userId}`

**接口描述**: 获取指定用户的主页信息（公开信息）

**是否需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "http://example.com/avatar.jpg",
    "intro": "这是我的个人简介",
    "articleCount": 10,
    "followerCount": 100,
    "followingCount": 50,
    "likeCount": 500,
    "isFollowed": false
  }
}
```

---

# 二、用户关注模块

## 2.1 关注用户

**接口地址**: `POST /api/user/follow/{userId}`

**接口描述**: 关注指定用户

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 被关注用户的ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "关注成功",
  "data": null
}
```

---

## 2.2 取消关注

**接口地址**: `DELETE /api/user/follow/{userId}`

**接口描述**: 取消关注指定用户

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 被关注用户的ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "取消关注成功",
  "data": null
}
```

---

## 2.3 获取关注列表

**接口地址**: `GET /api/user/follow/following`

**接口描述**: 获取当前用户关注的人列表

**是否需要认证**: 是

**请求参数**:

| 参数名      | 类型      | 必填 | 说明        |
|----------|---------|------|-----------|
| cursor   | Long    | 否 | 游标，初始为null |
| pageSize | Integer | 否 | 每页数量，默认20 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "list": [
      {
        "id": 2,
        "username": "lisi",
        "nickname": "李四",
        "avatar": "http://example.com/avatar2.jpg",
        "intro": "李四的简介",
        "followTime": "2026-05-12 10:00:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

---

## 2.4 获取粉丝列表

**接口地址**: `GET /api/user/follow/followers`

**接口描述**: 获取当前用户的粉丝列表

**是否需要认证**: 是

**请求参数**:

| 参数名      | 类型      | 必填 | 说明        |
|----------|---------|------|-----------|
| cursor   | Long    | 否 | 游标，初始为null |
| pageSize | Integer | 否 | 每页数量，默认20 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      {
        "id": 3,
        "username": "wangwu",
        "nickname": "王五",
        "avatar": "http://example.com/avatar3.jpg",
        "intro": "王五的简介",
        "followTime": "2026-05-12 10:00:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

---

# 三、文章模块

## 3.1 添加文章

**接口地址**: `POST /api/article`

**接口描述**: 创建并发布文章

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型         | 必填 | 说明                 |
|--------|------------|------|--------------------|
| title | String     | 是 | 文章标题，最多100字        |
| summary | String     | 否 | 文章摘要，不填自动截取内容前200字 |
| content | String     | 是 | 文章内容，支持Markdown格式  |
| categoryId | Long       | 是 | 分类ID               |
| tagIds | List<Long> | 否 | 标签ID列表             |
| isTop | Boolean    | 否 | 是否置顶，默认false       |
| status | String     | 是 | 草稿或发布             |                |

**请求示例**:
```json
{
  "title": "我的第一篇博客",
  "summary": "这是文章摘要",
  "content": "# 标题\n\n这是文章内容...",
  "categoryId": 1,
  "tagIds": [1, 2, 3],
  "isTop": false,
  "status": "草稿"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "发布成功",
  "data": {
    "id": 1,
    "title": "我的第一篇博客",
    "summary": "这是文章摘要",
    "authorId": "1",
    "authorName": "张三",
    "categoryId": 1,
    "categoryName": "技术",
    "tags": [1, 2, 3],
    "likeCount": 0,
    "commentCount": 0,
    "isTop": false,
    "status": "草稿",
    "updateTime": "2026-05-12 10:00:00"
  }
}
```

---

## 3.2 保存草稿

**接口地址**: `POST /api/article/draft`

**接口描述**: 保存文章为草稿状态

**是否需要认证**: 是

**请求参数**: 同发布文章接口，status固定为false

**请求示例**:
```json
{
  "title": "未完成的文章",
  "summary": "这是文章摘要",
  "content": "这是草稿内容...",
  "categoryId": 1,
  "tagIds": [1]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "草稿保存成功",
  "data": {
    "id": 2,
    "title": "未完成的文章",
    "status": false,
    "createTime": "2026-05-12 10:00:00"
  }
}
```

---

## 3.3 更新文章

**接口地址**: `PUT /api/article/update/{id}`

**接口描述**: 更新文章内容

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 文章ID |

**请求参数**: 同发布文章接口

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

---

## 3.4 删除文章

**接口地址**: `DELETE /api/article/delete/{id}`

**接口描述**: 删除文章（逻辑删除）

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 文章ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

## 3.5 获取文章详情

**接口地址**: `GET /api/article/{id}`

**接口描述**: 获取文章详细信息

**是否需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 文章ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "我的第一篇博客",
    "summary": "这是文章摘要",
    "content": "# 标题\n\n这是文章内容...",
    "author": 1,
    "nickname": "张三",
    "categoryId": 1,
    "categoryName": "技术",
    "tags": [
      {"id": 1, "name": "Java"},
      {"id": 2, "name": "Spring Boot"}
    ],
    "viewCount": 100,
    "likeCount": 50,
    "commentCount": 10,
    "isTop": false,
    "isFollowed": false,
    "isLiked": false,
    "createTime": "2026-05-12 10:00:00"
  }
}
```

---

## 3.6 获取文章列表

**接口地址**: `GET /api/article/list`

**接口描述**: 分页获取文章列表

**是否需要认证**: 否

**请求参数**:

| 参数名      | 类型      | 必填 | 说明   |
|----------|---------|------|---------------------|
| cursor   | Long    | 否 | 页码，默认null 表示第一页 |
| pageSize | Integer | 否 | 每页数量，默认10           |
| orderBy  | String  | 否 | 排序方式：create_time(默认),like_count |
| isAsc    | Boolean | 否 | 是否升序，默认false |

**请求示例**:
```json
{
  "cursor": null,
  "pageSize": 10,
  "orderBy": "create_time",
  "isAsc": false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "我的第一篇博客",
        "summary": "这是文章摘要",
        "author": {
          "id": 1,
          "nickname": "张三"
        },
        "categoryName": "技术",
        "tags": [
          {"id": 1, "name": "Java"}
        ],
        "viewCount": 100,
        "likeCount": 50,
        "commentCount": 10,
        "isTop": false,
        "updateTime": "2026-05-12 10:00:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

---

## 3.7 获取我的文章

**接口地址**: `GET /api/article/my`

**接口描述**: 获取当前用户发布的文章列表

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认10 |
| status | Boolean | 否 | 状态筛选：false=草稿，true=已发布 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "list": [
      {
        "id": 1,
        "title": "我的第一篇博客",
        "summary": "这是文章摘要",
        "status": true,
        "viewCount": 100,
        "likeCount": 50,
        "commentCount": 10,
        "createTime": "2026-05-12 10:00:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

---

## 3.9 发布草稿

**接口地址**: `PUT /api/article/publish/{id}`

**接口描述**: 将草稿文章发布

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 文章ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "发布成功",
  "data": null
}
```

---

## 3.10 置顶/取消置顶文章

**接口地址**: `PUT /api/article/top/{id}`

**接口描述**: 设置或取消文章置顶

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 文章ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| isTop | Boolean | 是 | 是否置顶 |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

# 四、文章点赞模块

## 4.1 点赞文章

**接口地址**: `POST /api/article/{id}/like`

**接口描述**: 点赞指定文章

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 文章ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "点赞成功",
  "data": {
    "likeCount": 51
  }
}
```

---


## 4.3 获取点赞用户列表

**接口地址**: `GET /api/article/{id}/likes`

**接口描述**: 获取点赞该文章的用户列表

**是否需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 文章ID |

**请求参数**:

| 参数名      | 类型      | 必填 | 说明      |
|----------|---------|----|---------|
| cursor   | Long    | 否  | 游标      |
| pageSize | Integer | 否  | 每次查询的大小 |


**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "list": [
      {
        "id": 2,
        "username": "lisi",
        "nickname": "李四",
        "avatar": "http://example.com/avatar.jpg"
      }
    ]
  }
}
```

---

# 五、评论模块

## 5.1 发表评论

**接口地址**: `POST /api/comment`

**接口描述**: 对文章发表评论

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明            |
|--------|------|----|---------------|
| articleId | Long | 是  | 文章ID          |
| content | String | 是  | 评论内容，最多500字   |
| parentId | Long | 是  | 父评论ID，评论文章时填0 |

**请求示例**:
```json
{
  "articleId": 1,
  "content": "写得真好！",
  "parentId": null
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "评论成功",
  "data": {
    "id": 1,
    "userId": 1,
    "parentId": null,
    "nickname": "张三",
    "avatar": "http://example.com/avatar.jpg",
    "content": "写得真好！",
    "isAuthor": true,
    "createTime": "2026-05-12 10:00:00"
  }
}
```

---

## 5.2 回复评论

**接口地址**: `POST /api/comment/reply`

**接口描述**: 回复某条评论

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| articleId | Long | 是 | 文章ID |
| content | String | 是 | 回复内容，最多500字 |
| parentId | Long | 是 | 父评论ID |

**请求示例**:
```json
{
  "articleId": 1,
  "content": "谢谢支持！",
  "parentId": 1
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "回复成功",
  "data": {
    "id": 2,
    "userId": 2,
    "parentId": 1,
    "nickname": "李四",
    "avatar": "http://example.com/avatar.jpg",
    "content": "评论的真好！",
    "isAuthor": false,
    "createTime": "2026-05-12 10:00:00"
  }
}
```

---

## 5.3 删除评论

**接口地址**: `DELETE /api/comment/{id}`

**接口描述**: 删除自己的评论

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 评论ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

## 5.4 获取文章评论列表

**接口地址**: `GET /api/comment/article/{articleId}`

**接口描述**: 获取指定文章的评论列表（两级结构：主评论 + 回复列表）

**是否需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| articleId | Long | 是 | 文章ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cursor | Long | 否 | 游标，首次传null或不传 |
| pageSize | Integer | 否 | 每页数量，默认20 |
| sortBy | String | 否 | 排序字段：create_time(默认), like_count |
| isAsc | Boolean | 否 | 是否升序，默认false(降序) |

**响应示例**:
```json
{ "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
      "id": 1,
        "parentId": 0,
        "userId": 1,
        "nickname": "张三",
        "avatar": "http://example.com/avatar.jpg",
        "content": "这篇文章写得真好！",
        "isAuthor": true,
        "createTime": "2026-05-12 10:00:00",
        "replies": [
          { "id": 2,
            "parentId": 1,
            "userId": 2,
            "nickname": "李四",
            "avatar": "http://example.com/avatar2.jpg",
            "content": "@张三 谢谢支持！",
            "replyToNickname": "张三",
            "isAuthor": false,
            "createTime": "2026-05-12 10:05:00"
          },
          { 
            "id": 3,
            "parentId": 1,
            "userId": 3,
            "nickname": "王五",
            "avatar": "http://example.com/avatar3.jpg",
            "content": "确实不错",
            "replyToNickname": "张三",
            "isAuthor": false,
            "createTime": "2026-05-12 10:10:00"
          } 
        ],
        "totalReplies": 2
      }
    ],
    "nextCursor": 90,
    "hasMore": true
  }
}
```

---

## 5.5 获取我的评论

**接口地址**: `GET /api/comment/my`

**接口描述**: 获取当前用户发表的评论列表

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 20,
    "list": [
      {
        "id": 1,
        "content": "写得真好！",
        "article": {
          "id": 1,
          "title": "我的第一篇博客"
        },
        "createTime": "2026-05-12 10:00:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

---

# 六、分类模块

## 6.1 获取分类列表

**接口地址**: `GET /api/category/list`

**接口描述**: 获取所有文章分类

**是否需要认证**: 否

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "技术",
      "sort": 1,
      "articleCount": 50,
      "createTime": "2026-05-12 10:00:00"
    },
    {
      "id": 2,
      "name": "生活",
      "sort": 2,
      "articleCount": 30,
      "createTime": "2026-05-12 10:00:00"
    }
  ]
}
```

---

## 6.2 添加分类

**接口地址**: `POST /api/category`

**接口描述**: 添加新的文章分类（管理员）

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 分类名称，最多20字 |
| sort | Integer | 否 | 排序，默认0 |

**请求示例**:
```json
{
  "name": "技术",
  "sort": 1
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "id": 1,
    "name": "技术",
    "sort": 1
  }
}
```

---

## 6.3 更新分类

**接口地址**: `PUT /api/category/{id}`

**接口描述**: 更新分类信息（管理员）

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 分类ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|----|------|
| name | String | 否  | 分类名称 |
| sort | Integer | 否  | 排序 |

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

---

## 6.4 删除分类

**接口地址**: `DELETE /api/category/{id}`

**接口描述**: 删除分类（管理员）

**是否需要认证**: 是

**路径参数**:

| 参数名        | 类型 | 必填 | 说明 |
|------------|------|------|------|
| categoryId | Long | 是 | 分类ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

# 七、标签模块

## 7.1 获取标签列表

**接口地址**: `GET /api/tag/list`

**接口描述**: 获取所有标签

**是否需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 否 | 搜索关键词 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "Java"
    },
    {
      "id": 2,
      "name": "Spring Boot"
    }
  ]
}
```

---

## 7.2 添加标签

**接口地址**: `POST /api/tag`

**接口描述**: 添加新标签

**是否需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 标签名称，最多20字 |

**请求示例**:
```json
{
  "name": "Java"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "id": 1,
    "name": "Java"
  }
}
```

---

## 7.4 删除标签

**接口地址**: `DELETE /api/tag/{id}`

**接口描述**: 删除标签

**是否需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 标签ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

## 7.5 获取热门标签

**接口地址**: `GET /api/tag/hot`

**接口描述**: 获取使用量最多的热门标签

**是否需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | Integer | 否 | 数量限制，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "Java",
      "articleCount": 30
    },
    {
      "id": 2,
      "name": "Spring Boot",
      "articleCount": 25
    }
  ]
}
```

---

# 八、搜索模块

## 8.1 搜索文章

**接口地址**: `GET /api/search/article`

**接口描述**: 全文搜索文章

**是否需要认证**: 否

**请求参数**:

| 参数名      | 类型      | 必填 | 说明                 |
|----------|---------|------|--------------------|
| keyword  | String  | 是 | 搜索关键词（用户名或昵称）      |
| pageSize | Integer | 否 | 每页数量，默认20          |
| isAsc    | Boolean | 否 | 是否升序排序，默认false     |
| sortBy   | String  | 否 | 排序字段，默认create_time |
| cursor   | Long    | 否 | 游标，默认null          |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "title": "Java入门教程",
        "summary": "这是一篇Java入门教程...",
        "author": {
          "id": 1,
          "nickname": "张三"
        },
        "createTime": "2026-05-12 10:00:00"
      }
    ],
    "nextCursor": 1,
    "hasMore": true
  }
}
```

---

## 8.2 搜索用户

**接口地址**: `GET /api/search/user`

**接口描述**: 搜索用户

**是否需要认证**: 否

**请求参数**:

| 参数名      | 类型      | 必填 | 说明                 |
|----------|---------|------|--------------------|
| keyword  | String  | 是 | 搜索关键词（用户名或昵称）      |
| pageSize | Integer | 否 | 每页数量，默认20          |
| isAsc    | Boolean | 否 | 是否升序排序，默认false     |
| sortBy   | String  | 否 | 排序字段，默认create_time |
| cursor   | Long    | 否 | 游标，默认null          |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "nickname": "张三",
        "avatar": "http://example.com/avatar.jpg",
        "isFollowed": false
      }
    ],
    "nextCursor": 1,
    "hasMore": true
  }
}
```

---

# 九、文件上传模块

## 9.1 上传图片

**接口地址**: `POST /api/upload/image`

**接口描述**: 上传图片文件

**是否需要认证**: 是

**请求方式**: multipart/form-data

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 图片文件，支持jpg/png/gif格式，最大5MB |

**响应示例**:
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "url": "http://example.com/images/2026/05/12/abc123.jpg",
    "fileName": "abc123.jpg",
    "fileSize": 102400
  }
}
```

---

## 9.2 上传头像

**接口地址**: `POST /api/upload/avatar`

**接口描述**: 上传用户头像

**是否需要认证**: 是

**请求方式**: multipart/form-data

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 头像图片，支持jpg/png格式，最大2MB |

**响应示例**:
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "url": "http://example.com/avatar/user_1.jpg"
  }
}
```

---

# 十、统计模块

## 10.1 获取网站统计数据

**接口地址**: `GET /api/statistics`

**接口描述**: 获取网站整体统计数据

**是否需要认证**: 否

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "articleCount": 1000,
    "userCount": 500
  }
}
```

---

## 10.2 获取用户统计数据

**接口地址**: `GET /api/statistics/user`

**接口描述**: 获取当前用户的统计数据

**是否需要认证**: 是

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "articleCount": 10,
    "followerCount": 100,
    "followingCount": 50,
    "likeCount": 500,
    "commentCount": 20,
    "viewCount": 1000
  }
}
```

---

## 10.3 获取热门文章

**接口地址**: `GET /api/statistics/hot`

**接口描述**: 获取热门文章排行

**是否需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 否 | 排行类型：view(浏览量), like(点赞量), comment(评论量)，默认view |
| limit | Integer | 否 | 数量限制，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "热门文章标题",
      "viewCount": 10000,
      "likeCount": 500,
      "commentCount": 100
    }
  ]
}
```

---

# 附录

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户名已存在 |
| 1002 | 用户名或密码错误 |
| 1003 | 用户不存在 |
| 1004 | 账号已被禁用 |
| 1005 | 原密码错误 |
| 2001 | 文章不存在 |
| 2002 | 无权限操作此文章 |
| 2003 | 文章已删除 |
| 3001 | 评论不存在 |
| 3002 | 无权限操作此评论 |
| 4001 | 分类不存在 |
| 4002 | 分类名称已存在 |
| 5001 | 标签不存在 |
| 5002 | 标签名称已存在 |
| 6001 | 不能关注自己 |
| 6002 | 已经关注过该用户 |
| 7001 | 文件格式不支持 |
| 7002 | 文件大小超出限制 |
| 7003 | 文件上传失败 |

---

## 更新日志

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0.0 | 2026-05-12 | 初始版本，完成所有基础接口 |

---

**文档维护**: 开发团队  
**最后更新**: 2026-05-12
