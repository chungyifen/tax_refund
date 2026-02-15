<template>
  <div class="tags-view-container">
    <el-scrollbar class="tags-view-wrapper">
      <router-link
        v-for="tag in visitedViews"
        ref="tag"
        :key="tag.path"
        :class="isActive(tag) ? 'active' : ''"
        :to="{ path: tag.path, query: tag.query, fullPath: tag.fullPath }"
        class="tags-view-item"
        @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : ''"
        @contextmenu.prevent="openMenu(tag, $event)"
      >
        {{ tag.title }}
        <span v-if="!isAffix(tag)" class="el-icon-close" @click.prevent.stop="closeSelectedTag(tag)">
            <el-icon><Close /></el-icon>
        </span>
      </router-link>
    </el-scrollbar>
    <ul v-show="visible" :style="{left:left+'px',top:top+'px'}" class="contextmenu">
      <li @click="refreshSelectedTag(selectedTag)">刷新</li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">關閉</li>
      <li @click="closeOthersTags">關閉其他</li>
      <li @click="closeAllTags(selectedTag)">關閉全部</li>
    </ul>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore } from '@/stores/tagsView'
import { Close } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()

const visible = ref(false)
const top = ref(0)
const left = ref(0)
const selectedTag = ref({})
const affixTags = ref([])

const visitedViews = computed(() => tagsViewStore.visitedViews)
// const routes = computed(() => permissionStore.routes) // Need permission store routes for affix

watch(route, () => {
  addTags()
  // moveToCurrentTag()
})

watch(visible, (value) => {
  if (value) {
    document.body.addEventListener('click', closeMenu)
  } else {
    document.body.removeEventListener('click', closeMenu)
  }
})

const isActive = (tag) => {
  return tag.path === route.path
}

const isAffix = (tag) => {
  return tag.meta && tag.meta.affix
}

const addTags = () => {
    const { name } = route
    if (name) {
        tagsViewStore.addView(route)
    }
    return false
}

const refreshSelectedTag = (view) => {
    tagsViewStore.delCachedView(view).then(() => {
        const { fullPath } = view
        nextTick(() => {
            router.replace({
                path: '/redirect' + fullPath
            }).catch(err => {
                 // redirect page might not exist, basic reload
                 router.replace({ path: fullPath, query: { t: Date.now() }})
            })
        })
    })
}

const closeSelectedTag = (view) => {
    tagsViewStore.delView(view).then(({ visitedViews }) => {
        if (isActive(view)) {
            toLastView(visitedViews, view)
        }
    })
}

const closeOthersTags = () => {
    router.push(selectedTag.value)
    tagsViewStore.delOthersViews(selectedTag.value).then(() => {
        // moveToCurrentTag()
    })
}

const closeAllTags = (view) => {
    tagsViewStore.delAllViews().then(({ visitedViews }) => {
        if (affixTags.value.some(tag => tag.path === view.path)) {
            return
        }
        toLastView(visitedViews, view)
    })
}

const toLastView = (visitedViews, view) => {
    const latestView = visitedViews.slice(-1)[0]
    if (latestView) {
        router.push(latestView.fullPath)
    } else {
        // now the default is to redirect to the home page if there is no tags-view,
        // you can adjust it according to your needs.
        if (view.name === 'Dashboard') {
            // to reload home page
            router.replace({ path: '/redirect' + view.fullPath })
        } else {
            router.push('/')
        }
    }
}

const openMenu = (tag, e) => {
    const menuMinWidth = 105
    const offsetLeft = 15 // container margin left
    const offsetWidth = 1000 // container width
    const maxLeft = offsetWidth - menuMinWidth 
    const left15 = e.clientX - offsetLeft + 15 

    // left.value = left15 > maxLeft ? maxLeft : left15
    // Simple positioning
    left.value = e.clientX
    top.value = e.clientY
    visible.value = true
    selectedTag.value = tag
}

const closeMenu = () => {
    visible.value = false
}

onMounted(() => {
    // Init affix tags? For now just add current route
    addTags()
})
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: 34px;
  width: 100%;
  background: #fff;
  border-bottom: 1px solid #d8dce5;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, .12), 0 0 3px 0 rgba(0, 0, 0, .04);
  .tags-view-wrapper {
    .tags-view-item {
      display: inline-block;
      position: relative;
      cursor: pointer;
      height: 26px;
      line-height: 26px;
      border: 1px solid #d8dce5;
      color: #495060;
      background: #fff;
      padding: 0 8px;
      font-size: 12px;
      margin-left: 5px;
      margin-top: 4px;
      text-decoration: none;
      &:first-of-type {
        margin-left: 15px;
      }
      &:last-of-type {
        margin-right: 15px;
      }
      &.active {
        background-color: #42b983;
        color: #fff;
        border-color: #42b983;
        &::before {
          content: '';
          background: #fff;
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          position: relative;
          margin-right: 2px;
        }
      }
    }
  }
  .contextmenu {
    margin: 0;
    background: #fff;
    z-index: 3000;
    position: absolute;
    list-style-type: none;
    padding: 5px 0;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 400;
    color: #333;
    box-shadow: 2px 2px 3px 0 rgba(0, 0, 0, .3);
    li {
      margin: 0;
      padding: 7px 16px;
      cursor: pointer;
      &:hover {
        background: #eee;
      }
    }
  }
}
</style>
