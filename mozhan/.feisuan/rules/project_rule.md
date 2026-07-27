
# 开发规范指南

为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、技术栈要求

- **主框架**：Spring Boot 3.5.14
- **语言版本**：Java 21.0.7
- **核心依赖**：
  - `spring-boot-starter-web`
  - `mybatis-plus-boot-starter` (3.5.7)
  - `lombok` (1.18.44)
  - `hutool-all` (5.8.27)
  - `mysql-connector-j`

## 二、项目目录结构

项目工作目录：`D:\javaCode\mozhan\mozhan`

```text
mozhan
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── god
    │   │           └── mz
    │   │               ├── config          // 配置类
    │   │               ├── controller       // 控制层
    │   │               ├── domain           // 领域对象
    │   │               │   ├── dto          // 数据传输对象
    │   │               │   ├── po           // 持久化对象
    │   │               │   ├── query        // 查询参数对象
    │   │               │   └── vo           // 视图展示对象
    │   │               ├── exception        // 异常处理
    │   │               ├── mapper           // MyBatis-Plus Mapper接口
    │   │               ├── service          // 业务层
    │   │               │   └── impl         // 业务层实现
    │   │               └── util             // 工具类
    │   └── resources
    │       ├── mapper        // XML映射文件
    │       ├── static        // 静态资源
    │       └── templates     // 模板文件
    └── test
        └── java
            └── com
                └── god
                    └── mz
```

## 三、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|-----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 不得直接访问数据库；必须通过 Service 层调用；返回 `VO` 或 `DTO`    |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 必须通过 Mapper 层访问数据库；返回 DTO/VO/PO；**禁止**直接返回 Entity |
| **Mapper**     | 数据库访问与持久化操作             | 继承 `BaseMapper`；使用 MyBatis-Plus 注解或 XML 文件进行操作       |
| **Entity**     | 映射数据库表结构                   | 不得直接返回给前端（需转换为 DTO/VO）；包名统一为 `domain.po`     |
| **Domain**     | 数据对象封装                       | 包含 `DTO`, `VO`, `Query` 等子包；**禁止** Entity 直接暴露         |

### 接口与实现分离

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `service.impl` 子包中。

## 四、安全与性能规范

### 输入校验

- 使用 `@Valid` 与 JSR-303 校验注解（如 `@NotBlank`, `@Size` 等）
  - 注意：Spring Boot 3.x 中校验注解位于 `jakarta.validation.constraints.*`

- 禁止手动拼接 SQL 字符串，防止 SQL 注入攻击。

### 事务管理

- `@Transactional` 注解仅用于 **Service 层**方法。
- 避免在循环中频繁提交事务，影响性能。

## 五、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |

### 注释规范

- 所有类、方法、字段需添加 **Javadoc** 注释。
- 注释语言：**中文**。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| PO   | 持久化对象（等同于Entity）    | `UserPO`     |
| VO   | 视图展示对象                 | `UserVO`     |
| Query| 查询参数封装对象             | `UserQuery`  |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`。

### MyBatis-Plus 规范

- Mapper 接口继承 `BaseMapper`。
- 实体类需添加 `@TableName` 注解映射表名。
- 避免在 Service 层大量使用 `new` 创建对象，尽量复用。

## 六、扩展性与日志规范

### 接口优先原则

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `service.impl` 子包中。

### 日志记录

- 使用 `@Slf4j` 注解代替 `System.out.println`
- 日志级别使用：`info`（业务完成）、`debug`（逻辑调试）、`error`（异常捕获）。

## 七、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |

## 八、开发人员信息

- **作者**：ASUS
