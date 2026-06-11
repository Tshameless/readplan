<template>
  <div class="state-page">
    <el-result icon="info" sub-title="你访问的页面不存在，或者路由还没有被创建。" title="404 页面不存在">
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">回到首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped lang="scss">
.state-page {
  display: grid;
  min-height: 100%;
  place-items: center;
}
</style>
