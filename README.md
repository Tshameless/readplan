# ReadPlan

`ReadPlan` 是一个个人阅读计划与笔记系统，当前仓库已经具备可运行的前后端联调版本。

## 项目结构

- `readplan-web`
  - Vue 3 + Vite + TypeScript + Element Plus
- `readplan-server`
  - Spring Boot 3.5 + Spring Security + JWT + MyBatis-Plus + MySQL

## 当前能力

### 前端 `readplan-web`

- Vue 3 + Vite + TypeScript + Element Plus
- 已接通真实后端接口，不再使用本地 mock
- 已完成页面：
  - 书籍广场
  - 书籍详情
  - 我的阅读计划
  - 我的笔记
  - 后台管理
  - 登录 / 注册
- 已完成交互：
  - 登录、注册、鉴权失效跳转
  - 浏览书籍与公开笔记
  - 加入阅读计划、修改状态、移除计划
  - 创建 / 编辑 / 删除个人笔记
  - 查看评论、发表评论、删除可管理评论
  - 管理员导入候选书籍、手动新增、编辑、下架书籍

### 后端 `readplan-server`

- Spring Boot 3 + Spring Security + JWT + MyBatis-Plus + MySQL
- 已完成：
  - 统一返回结构
  - 全局异常处理
  - JWT 登录鉴权
  - 基于角色的后台接口保护
  - Auth / User / Book / Plan / Note / Comment 接口
  - `MySQL + MyBatis-Plus` 实体、Mapper 和数据初始化
  - 首次启动自动建表并写入演示数据
- 数据库脚本：
  - [db/schema.sql](C:/Users/Administrator/Documents/Codex/2026-06-11/a/readplan-server/db/schema.sql)

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18+
- pnpm 10+
- MySQL 8.x

## 启动说明

### 1. 准备 MySQL

项目默认连接：

- 数据库：`readplan`
- 用户名：`root`
- 密码：`123456`

对应配置文件：

- [application.yml](C:/Users/Administrator/Documents/Codex/2026-06-11/a/readplan-server/src/main/resources/application.yml)

默认 JDBC 地址：

```yaml
jdbc:mysql://localhost:3306/readplan?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
```

说明：

- 如果本地没有 `readplan` 数据库，后端启动时会自动创建。
- 启动时会自动执行 [schema.sql](C:/Users/Administrator/Documents/Codex/2026-06-11/a/readplan-server/db/schema.sql) 建表。
- 当库里没有用户数据时，会自动写入演示账号、演示书籍、阅读计划、笔记和评论。

### 2. 启动后端

```bash
cd readplan-server
mvn spring-boot:run
```

后端默认地址：

- `http://localhost:8080`
- Swagger / Knife4j：`http://localhost:8080/doc.html`

### 3. 启动前端

```bash
cd readplan-web
pnpm install
pnpm dev
```

前端默认地址：

- `http://localhost:5173`

### 4. 联调访问

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`

## 当前账号

- 普通用户：`reader / 123456`
- 管理员：`admin / 123456`
- 其他演示用户：`zhangsan / 123456`、`aqing / 123456`、`architect / 123456`

## 开发命令

### 前端

```bash
cd readplan-web
pnpm dev
pnpm type-check
pnpm build
```

### 后端

```bash
cd readplan-server
mvn compile
mvn test
mvn spring-boot:run
```

## 已验证

- 前端：
  - `pnpm type-check`
  - `pnpm build`
- 后端：
  - `mvn -DskipTests compile`
  - `mvn test`
  - 登录、用户信息、书单、计划、笔记、评论、后台书库接口烟测

## 后端测试

已补充的后端集成测试位于：

- [ReadPlanAuthIntegrationTest.java](C:/Users/Administrator/Documents/Codex/2026-06-11/a/readplan-server/src/test/java/com/readplan/ReadPlanAuthIntegrationTest.java)
- [ReadPlanFlowIntegrationTest.java](C:/Users/Administrator/Documents/Codex/2026-06-11/a/readplan-server/src/test/java/com/readplan/ReadPlanFlowIntegrationTest.java)

覆盖内容：

- 登录并获取 JWT
- 受保护接口鉴权
- 公共书单接口访问
- 阅读计划创建、更新、删除
- 笔记创建、更新、删除
- 评论创建、查询、删除

## 仍可继续增强

下面这些还没有完全做成生产版：

1. 接入真实 Open Library 远程搜索，而不是本地候选数据。
2. 继续补充更细粒度的后端测试和前端 E2E 测试。
3. 增加更完整的分页、筛选和表单校验。
4. 增加 Docker / docker-compose 一键启动方式。
