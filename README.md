# ReadPlan

`ReadPlan` 是一个个人阅读计划与笔记系统，当前仓库已经具备可运行的前后端联调版本。

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

- Spring Boot 3 + Spring Security + JWT + MyBatis-Plus 依赖骨架
- 已完成：
  - 统一返回结构
  - 全局异常处理
  - JWT 登录鉴权
  - 基于角色的后台接口保护
  - Auth / User / Book / Plan / Note / Comment 接口
  - 内存业务数据存储，支持主要业务流联调
- 数据库脚本：
  - [db/schema.sql](C:/Users/Administrator/Documents/Codex/2026-06-11/a/readplan-server/db/schema.sql)

## 启动方式

1. 前端

```bash
cd readplan-web
pnpm install
pnpm dev
```

2. 后端

```bash
cd readplan-server
mvn spring-boot:run
```

默认地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`

## 当前账号

- 普通用户：`reader / 123456`
- 管理员：`admin / 123456`

## 已验证

- 前端：
  - `pnpm type-check`
  - `pnpm lint`
  - `pnpm lint:style`
  - `pnpm build`
- 后端：
  - `mvn -DskipTests compile`
  - 登录、用户信息、计划、笔记、评论、后台书库接口烟测

## 仍可继续增强

下面这些还没有完全做成生产版：

1. 把后端内存数据切换为 `MySQL + MyBatis-Plus` 实体 / Mapper / Service。
2. 接入真实 Open Library 远程搜索，而不是本地候选数据。
3. 补充自动化测试，尤其是后端 service / controller 和前端关键交互。
4. 增加更完整的表单校验、分页和更细的权限提示。
