<template>
  <div class="state-page">
    <el-result icon="warning" sub-title="当前账号没有访问这个页面的权限。" title="403 无权限">
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
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
