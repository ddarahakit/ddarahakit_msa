<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import useAuthStore from '@/stores/useAuthStore'
import useCartStore from '@/stores/useCartStore'
import { useRoute, useRouter } from 'vue-router'
import HeaderMenu from './HeaderMenu.vue'
import UserAvatar from '@/components/base/UserAvatar.vue'

//라우터 정보 객체
const router = useRouter()

//현재 라우트 정보 객체
const route = useRoute()

// '강의 탐색' 활성화: 강의 목록/카테고리/상세 등 모든 /course 경로에서 활성
// (카테고리 선택 시 courseListByCategory 라우트라 기본 active-class 가 안 잡히는 문제 해결)
const isCourseActive = computed(() => route.path.startsWith('/course'))

// '멘토링' 활성화: /mentoring 하위 모든 경로에서 활성
const isMentoringActive = computed(() => route.path.startsWith('/mentoring'))

// '로드맵' 활성화: /roadmap 하위 모든 경로에서 활성
const isRoadmapActive = computed(() => route.path.startsWith('/roadmap'))

// '포트폴리오' 활성화: /projects 하위 모든 경로에서 활성
const isProjectActive = computed(() => route.path.startsWith('/projects'))

// '커뮤니티' 활성화: /community 하위 모든 경로에서 활성
const isCommunityActive = computed(() => route.path.startsWith('/community'))

//강의 검색
const searchKeyword = ref('')
const goSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  router.push({ name: 'courseList', query: { keyword: kw } })
}

//저장소 정보 객체
const authStore = useAuthStore()

//장바구니 저장소 (헤더 배지 ↔ 장바구니 화면 공유)
const cartStore = useCartStore()

//props 설정
const props = defineProps({
  isScrolled: { type: Boolean, required: true },
});

//헤더 메뉴 표시 여부
const isHeaderSideMenuVisible = ref(false)

// 프로필 메뉴 표시 여부
const isMenuOpen = ref(false)

const emit = defineEmits(['update:isHeaderSideMenuVisible'])

/**
 * 로그아웃
 *
 * 실제 로그아웃 처리는 전용 페이지(/logout)에서 수행한다.
 */
const logout = () => {
  router.push({ name: 'logout' })
}

const showHeaderMenu = () => {
  isHeaderSideMenuVisible.value = !isHeaderSideMenuVisible.value
}

const closeHeaderMenu = () => {
  isHeaderSideMenuVisible.value = false
}

/**
 * 컴포넌트 마운트
 *
 * 로그인 정보 초기 설정
 * 새로고침 시 afterEach 훅은 작동하지 않는다.
 */
// 로그인 상태 변경 감지 → 장바구니 카운트 조회
watch(() => authStore.isLogin, (loggedIn) => {
  if (loggedIn) {
    cartStore.fetchCount()
  } else {
    cartStore.reset()
  }
})

onMounted(() => {
  //로그인 정보 초기화
  authStore.initSettings()
  //장바구니 항목 수 조회
  if (authStore.isLogin) {
    cartStore.fetchCount()
  }
})
</script>

<template>
  <nav class="glass-nav fixed w-full z-50 px-6">
    <div class="max-w-7xl mx-auto flex justify-between items-center">
      <div class="flex items-center gap-8">
        <a href="/" class="text-2xl font-bold">
          따라하면서 배우는 <span class="text-brand">IT</span>
        </a>
        <div class="hidden md:flex items-center gap-6 text-sm font-medium text-gray-500">
          <RouterLink :to="{ name: 'courseList' }" class="hover:text-brand transition-colors py-5 border-b-2 border-transparent"
            :class="{ '!text-brand !border-brand': isCourseActive }">실제 수업</RouterLink>
          <RouterLink :to="{ name: 'mentoring' }" class="hover:text-brand transition-colors py-5 border-b-2 border-transparent"
            :class="{ '!text-brand !border-brand': isMentoringActive }">멘토링</RouterLink>
          <RouterLink :to="{ name: 'roadmap' }" class="hover:text-brand transition-colors py-5 border-b-2 border-transparent"
            :class="{ '!text-brand !border-brand': isRoadmapActive }">로드맵</RouterLink>
          <RouterLink :to="{ name: 'projectManagement' }" class="hover:text-brand transition-colors py-5 border-b-2 border-transparent"
            :class="{ '!text-brand !border-brand': isProjectActive }">포트폴리오</RouterLink>
          <RouterLink :to="{ name: 'communityList' }" class="hover:text-brand transition-colors py-5 border-b-2 border-transparent"
            :class="{ '!text-brand !border-brand': isCommunityActive }">커뮤니티</RouterLink>
        </div>
      </div>
      <!-- 강의 검색 -->
      <form @submit.prevent="goSearch" class="hidden lg:flex items-center bg-gray-100/80 rounded-full px-4 py-2 w-64">
        <i class="fa-solid fa-magnifying-glass text-gray-400 text-sm"></i>
        <input v-model="searchKeyword" type="search" placeholder="강의 검색"
          class="bg-transparent outline-none text-sm ml-2 w-full text-gray-700 placeholder-gray-400" />
      </form>
      <!-- 우측: 로그인/프로필 영역 + 모바일 햄버거 -->
      <div class="flex items-center gap-3">
      <div v-if="authStore.isLogin" class="flex items-center gap-5">
        <RouterLink :to="{ name: 'ordersCart' }" class="relative text-slate-400 hover:text-slate-600">
          <i class="fa-solid fa-cart-shopping"></i>
          <span v-if="cartStore.count > 0"
            class="absolute -top-2 -right-2 bg-brand text-white text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
            {{ cartStore.count }}
          </span>
        </RouterLink>
        <div class="w-px h-4 bg-slate-200"></div>
        <div class="relative" @mouseenter="isMenuOpen = true" @mouseleave="isMenuOpen = false">
          <!-- 프로필 영역 -->
          <div class="flex items-center py-0.5 gap-3 cursor-pointer">
            <div class="text-right hidden sm:block">
              <p class="text-[13px] font-bold text-slate-700 leading-none">
                {{ authStore.getUserName() }}
              </p>
              <p class="text-[10px] text-slate-400 mt-1">LV.2 수강생</p>
            </div>

            <div class="w-9 h-9 rounded-full bg-slate-100 border border-slate-200 p-0.5">
              <UserAvatar :src="authStore.profileImage" rounded="rounded-full" icon-class="text-sm" />
            </div>
          </div>

          <!-- 드롭다운 메뉴 -->
          <div v-show="isMenuOpen" class="absolute top-full right-0 pt-2 z-[60]">
            <!-- 실제 메뉴 박스 -->
            <div class="w-48 bg-white border border-slate-100 rounded-2xl shadow-xl py-2">
              <RouterLink class="block px-4 py-2 text-sm hover:bg-slate-50" :to="{ name: 'dashboard' }">
                대시보드
              </RouterLink>

              <hr class="my-1 border-slate-100" />

              <button @click="logout()" class="w-full text-left px-4 py-2 text-sm text-red-500 hover:bg-red-50">
                로그아웃
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="flex items-center gap-4">
        <RouterLink :to="{ name: 'login' }"
          class="px-4 py-2.5 text-sm font-medium text-gray-600 hover:text-brand transition-colors">로그인</RouterLink>
        <RouterLink :to="{ name: 'signup' }"
          class="px-5 py-2.5 bg-brand text-white rounded-full text-sm font-bold shadow-lg shadow-blue-100">시작하기
        </RouterLink>
      </div>

        <!-- 모바일 햄버거 메뉴 버튼 (PC는 숨김, 데스크톱 nav 사용) -->
        <button type="button" @click="showHeaderMenu" aria-label="메뉴 열기"
          class="md:hidden w-9 h-9 flex items-center justify-center text-slate-600 hover:text-brand">
          <i class="fa-solid fa-bars text-lg"></i>
        </button>
      </div>
    </div>
  </nav>
  <HeaderMenu :isHeaderMenuVisible="isHeaderSideMenuVisible" @closeHeaderMenu="closeHeaderMenu"
    :isLogin="authStore.isLogin" />
</template>
<style scoped>
.glass-nav {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}
</style>
