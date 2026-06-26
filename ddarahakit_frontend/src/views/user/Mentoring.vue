<script setup>
import api from '@/api/mentoring'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SessionList from '@/components/user/mentoring/SessionList.vue'
import SessionDetail from '@/components/user/mentoring/SessionDetail.vue'
import SessionDefault from '@/components/user/mentoring/SessionDefault.vue'

const route = useRoute()
const router = useRouter()

const mentoringList = reactive([])
const scheduleDetail = ref(null)

const isHistoryPage = computed(() => route.name === 'mentoringHistory')
const isSchedulePage = computed(() =>
  route.name === 'mentoringSchedule' || route.name === 'mentoringScheduleDetail',
)

const selectedSessionId = computed(() => route.params.sessionId)
const selectedScheduleId = computed(() => route.params.sessionId)

const getSessionId = (session) => {
  return session.id
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

    <main v-if="isHistoryPage" class="flex-1 flex overflow-hidden">
      <SessionList
        :sessions="mentoringList"
        :selected-session-id="selectedSessionId"
        @select="selectHistorySession" />

      <section class="flex-1 flex flex-col bg-slate-50 overflow-hidden">
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

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <button
          type="button"
          @click="openScheduleSession(session)"
          v-for="session in mentoringList"
          :key="`schedule-${getSessionId(session)}`"
          class="text-left bg-white border border-slate-100 rounded-2xl p-5 shadow-sm hover:border-brand/40 transition-all"
          :class="String(getSessionId(session)) === String(selectedScheduleId) ? 'ring-2 ring-blue-100 border-brand/30' : ''">
          <h3 class="font-bold text-slate-800 mb-2">{{ session.subject || session.title || '멘토링 세션' }}</h3>
          <p class="text-xs text-slate-500 mb-2">일정: {{ session.scheduledAt || session.sessionDate || session.updatedAt || '미정' }}</p>
          <p class="text-xs text-slate-400">멘토: {{ session.mentorName || '미정' }}</p>
        </button>
      </div>

      <section v-if="selectedScheduleId" class="mt-6 bg-white border border-slate-100 rounded-2xl p-6 shadow-sm">
        <h3 class="text-base font-bold text-slate-800 mb-3">일정 상세</h3>
        <p class="text-sm text-slate-700 mb-2">
          주제: {{ scheduleDetail?.subject || scheduleDetail?.title || '멘토링 세션' }}
        </p>
        <p class="text-sm text-slate-500 mb-2">
          일정: {{ scheduleDetail?.scheduledAt || scheduleDetail?.sessionDate || scheduleDetail?.updatedAt || '미정' }}
        </p>
        <p class="text-sm text-slate-500">
          멘토: {{ scheduleDetail?.mentorName || '미정' }}
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
</style>
