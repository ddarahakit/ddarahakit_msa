<script setup>
import api from '@/api/mentoring'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import useAuthStore from '@/stores/useAuthStore'
import SessionList from '@/components/user/mentoring/SessionList.vue'
import SessionDetail from '@/components/user/mentoring/SessionDetail.vue'
import SessionDefault from '@/components/user/mentoring/SessionDefault.vue'
import MentoringAvatar from '@/components/user/mentoring/MentoringAvatar.vue'
import { getCounterpart, formatSchedule } from '@/utils/mentoring'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const myIdx = authStore.getUserIdx()
const partyOf = (session) => getCounterpart(session, myIdx)

// 세션 목록 사이드바 열림/닫힘 (강의 수강 페이지처럼 토글, localStorage 유지)
const SIDEBAR_KEY = 'mentoringSidebarOpen'
const sidebarOpen = ref(localStorage.getItem(SIDEBAR_KEY) !== 'false')
const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
  localStorage.setItem(SIDEBAR_KEY, String(sidebarOpen.value))
}

const mentoringList = reactive([])
const scheduleDetail = ref(null)

const isHistoryPage = computed(() => route.name === 'mentoringHistory')
const isSchedulePage = computed(() =>
  route.name === 'mentoringSchedule' || route.name === 'mentoringScheduleDetail',
)

const selectedSessionId = computed(() => route.params.sessionId)
const selectedScheduleId = computed(() => route.params.sessionId)

const getSessionId = (session) => {
  return session.sessionIdx
    || session.id
    || session.sessionId
    || session.session_id
    || session.idx
    || session.mentoringIdx
    || session.mentoring_id
    || session.chatRoomId
}

const getList = async () => {
  const res = await api.list()
  const result = res?.results || res?.result || []
  const list = Array.isArray(result)
    ? result
    : Array.isArray(result?.list)
      ? result.list
      : Array.isArray(result?.items)
        ? result.items
        : []

  mentoringList.length = 0
  mentoringList.push(...list)
}

const moveToHistory = () => {
  router.push({ name: 'mentoringHistory' })
}

const moveToSchedule = () => {
  router.push({ name: 'mentoringSchedule' })
}

const selectHistorySession = (session) => {
  const sessionId = getSessionId(session)
  if (!sessionId) return
  router.push({
    name: 'mentoringHistory',
    params: { sessionId: String(sessionId) }
  })
}

const openScheduleSession = (session) => {
  const sessionId = getSessionId(session)
  if (!sessionId) return
  router.push({
    name: 'mentoringScheduleDetail',
    params: { sessionId: String(sessionId) }
  })
}

const loadScheduleDetail = async () => {
  if (!selectedScheduleId.value) {
    scheduleDetail.value = null
    return
  }

  const selectedInList = mentoringList.find(
    (item) => String(getSessionId(item)) === String(selectedScheduleId.value),
  )
  if (selectedInList) {
    scheduleDetail.value = selectedInList
  }

  const res = await api.detail(selectedScheduleId.value)
  const result = res?.results || res?.result
  if (result && !Array.isArray(result)) {
    scheduleDetail.value = { ...scheduleDetail.value, ...result }
  }
}

onMounted(async () => {
  await getList()
  if (isSchedulePage.value) {
    loadScheduleDetail()
  }
})

watch(
  () => route.params.sessionId,
  () => {
    if (isSchedulePage.value) {
      loadScheduleDetail()
    }
  },
)
</script>

<template>
  <div class="h-[calc(100vh-80px)] flex overflow-hidden pt-20">

    <main v-if="isHistoryPage" class="flex-1 flex overflow-hidden relative">
      <!-- 접을 수 있는 세션 목록 사이드바 -->
      <div class="mentoring-sidebar shrink-0" :class="{ closed: !sidebarOpen }">
        <SessionList
          :sessions="mentoringList"
          :selected-session-id="selectedSessionId"
          @select="selectHistorySession"
          @toggle="toggleSidebar" />
      </div>

      <!-- 닫혔을 때 다시 여는 탭 -->
      <button
        v-show="!sidebarOpen"
        type="button"
        @click="toggleSidebar"
        title="세션 목록 열기"
        class="absolute left-0 top-1/2 -translate-y-1/2 z-30 w-6 h-16 bg-white border border-l-0 border-slate-200 rounded-r-xl shadow-md flex items-center justify-center text-slate-400 hover:text-brand transition-colors">
        <i class="fa-solid fa-angles-right text-xs"></i>
      </button>

      <section class="flex-1 flex flex-col bg-slate-50 overflow-hidden min-w-0">
        <SessionDetail
          v-if="selectedSessionId"
          :key="String(selectedSessionId)"
          :session-id="selectedSessionId" />
        <SessionDefault v-else />
      </section>
    </main>

    <main v-else class="flex-1 overflow-y-auto p-6 bg-slate-50">
      <div class="flex items-center gap-3 mb-4">
        <button
          @click="moveToHistory"
          class="px-4 py-2 rounded-xl text-sm font-bold transition-all text-slate-500 hover:bg-slate-100">
          멘토링 기록
        </button>
        <button
          @click="moveToSchedule"
          class="px-4 py-2 rounded-xl text-sm font-bold transition-all bg-blue-50 text-brand">
          멘토링 일정
        </button>
      </div>

      <div v-if="mentoringList.length" class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <button
          type="button"
          @click="openScheduleSession(session)"
          v-for="session in mentoringList"
          :key="`schedule-${getSessionId(session)}`"
          class="text-left bg-white border border-slate-100 rounded-2xl p-5 shadow-sm hover:border-brand/40 hover:shadow-md transition-all"
          :class="String(getSessionId(session)) === String(selectedScheduleId) ? 'ring-2 ring-blue-100 border-brand/30' : ''">
          <div class="flex items-start justify-between gap-2 mb-3">
            <div class="flex items-center gap-2.5 min-w-0">
              <MentoringAvatar
                :name="partyOf(session).other?.name"
                :idx="partyOf(session).other?.idx"
                :image="partyOf(session).other?.profileImageUrl"
                :size="36" />
              <div class="min-w-0">
                <p class="text-sm font-bold text-slate-800 truncate">{{ partyOf(session).other?.name || '상대' }}</p>
                <span class="text-[10px] font-semibold"
                  :class="partyOf(session).otherRole === '멘토' ? 'text-violet-600' : 'text-emerald-600'">
                  {{ partyOf(session).otherRole }}
                </span>
              </div>
            </div>
            <span class="text-[9px] font-bold px-2 py-0.5 rounded-full shrink-0"
              :class="(session.status || 'OPEN') === 'OPEN' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-400'">
              {{ (session.status || 'OPEN') === 'OPEN' ? '진행중' : '종료' }}
            </span>
          </div>
          <h3 class="font-bold text-slate-800 mb-2 line-clamp-1">{{ session.subject || session.title || '멘토링 세션' }}</h3>
          <p class="text-xs text-slate-500 flex items-center gap-1.5">
            <i class="fa-regular fa-calendar text-slate-300"></i>
            {{ formatSchedule(session.scheduledAt) }}
          </p>
        </button>
      </div>

      <div v-else class="flex flex-col items-center justify-center text-center py-20 text-slate-400">
        <div class="w-16 h-16 rounded-2xl bg-white shadow-sm border border-slate-100 flex items-center justify-center mb-4">
          <i class="fa-regular fa-calendar text-2xl text-brand"></i>
        </div>
        <p class="text-sm font-medium text-slate-500">예정된 멘토링 일정이 없습니다</p>
      </div>

      <section v-if="selectedScheduleId && scheduleDetail" class="mt-6 bg-white border border-slate-100 rounded-2xl p-6 shadow-sm">
        <div class="flex items-center gap-3 mb-4 pb-4 border-b border-slate-100">
          <MentoringAvatar
            :name="partyOf(scheduleDetail).other?.name"
            :idx="partyOf(scheduleDetail).other?.idx"
            :image="partyOf(scheduleDetail).other?.profileImageUrl"
            :size="44" />
          <div>
            <p class="text-sm font-bold text-slate-800">{{ partyOf(scheduleDetail).other?.name || '상대' }}</p>
            <span class="text-[11px] font-semibold"
              :class="partyOf(scheduleDetail).otherRole === '멘토' ? 'text-violet-600' : 'text-emerald-600'">
              {{ partyOf(scheduleDetail).otherRole }}
            </span>
          </div>
        </div>
        <p class="text-sm text-slate-700 mb-2">
          <span class="text-slate-400">주제</span>
          &nbsp;{{ scheduleDetail?.subject || scheduleDetail?.title || '멘토링 세션' }}
        </p>
        <p class="text-sm text-slate-500">
          <span class="text-slate-400">일정</span>
          &nbsp;{{ formatSchedule(scheduleDetail?.scheduledAt) }}
        </p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}

/* 세션 목록 사이드바: 접으면 좌측으로 슬라이드되어 숨고 대화 영역이 넓어진다 */
.mentoring-sidebar {
  width: 20rem;
  height: 100%;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mentoring-sidebar.closed {
  margin-left: -20rem;
}
</style>
