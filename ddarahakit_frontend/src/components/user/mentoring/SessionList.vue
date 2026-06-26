<script setup>
import { computed, ref } from 'vue'
import useAuthStore from '@/stores/useAuthStore'
import MentoringAvatar from '@/components/user/mentoring/MentoringAvatar.vue'
import { getCounterpart, formatTimeAgo, hasUnread } from '@/utils/mentoring'

const props = defineProps({
    sessions: {
        type: Array,
        default: () => []
    },
    selectedSessionId: {
        type: [String, Number],
        default: null
    }
})

const emits = defineEmits(['select'])
const keyword = ref('')

const authStore = useAuthStore()
const myIdx = authStore.getUserIdx()

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

// 표시용으로 가공한 세션(상대방/역할/안읽음/시간)
const viewSessions = computed(() =>
    props.sessions.map((session) => {
        const { other, otherRole } = getCounterpart(session, myIdx)
        return {
            raw: session,
            id: getSessionId(session),
            subject: session.subject || session.title || '멘토링 세션',
            lastMessage: session.lastMessage || '아직 대화가 없습니다.',
            time: formatTimeAgo(session.lastMessageAt),
            status: session.status || 'OPEN',
            otherName: other?.name || '알 수 없음',
            otherIdx: other?.idx ?? null,
            otherImage: other?.profileImageUrl || '',
            otherRole,
            unread: hasUnread(session, myIdx),
        }
    })
)

const filteredSessions = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    if (!q) return viewSessions.value
    return viewSessions.value.filter((s) =>
        s.subject.toLowerCase().includes(q) ||
        s.otherName.toLowerCase().includes(q) ||
        String(s.lastMessage).toLowerCase().includes(q)
    )
})

const openCount = computed(() => viewSessions.value.filter((s) => s.status === 'OPEN').length)

const onSelect = (item) => {
    emits('select', item.raw)
}
</script>

<template>
    <section class="w-full lg:w-80 bg-white border-r border-slate-200 flex flex-col shrink-0">
        <!-- 헤더 -->
        <div class="p-5 border-b border-slate-100">
            <div class="flex items-center justify-between mb-4">
                <div class="flex items-center gap-2">
                    <h2 class="font-extrabold text-lg text-slate-800">멘토링</h2>
                    <span class="text-[11px] font-bold text-brand bg-blue-50 px-2 py-0.5 rounded-full">
                        진행 {{ openCount }}
                    </span>
                </div>
                <span class="text-[11px] text-slate-400">전체 {{ viewSessions.length }}</span>
            </div>
            <div class="relative">
                <i class="fa-solid fa-magnifying-glass absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-300 text-xs"></i>
                <input
                    v-model="keyword"
                    type="text"
                    placeholder="세션·상대방 검색..."
                    class="w-full pl-9 pr-3 py-2.5 bg-slate-100 border-none rounded-xl text-sm outline-none focus:ring-2 focus:ring-brand/30 transition-all">
            </div>
        </div>

        <!-- 세션 목록 -->
        <div id="session-list" class="flex-1 overflow-y-auto custom-scrollbar">
            <button
                v-for="(item, index) in filteredSessions"
                :key="String(item.id || `session-${index}`)"
                type="button"
                @click="onSelect(item)"
                class="w-full text-left flex gap-3 p-4 transition-all border-b border-slate-50 hover:bg-slate-50"
                :class="String(item.id) === String(selectedSessionId) ? 'bg-blue-50/60' : ''">
                <!-- 선택 표시 바 -->
                <span
                    class="w-1 rounded-full -my-1 shrink-0 transition-all"
                    :class="String(item.id) === String(selectedSessionId) ? 'bg-brand' : 'bg-transparent'"></span>

                <MentoringAvatar :name="item.otherName" :idx="item.otherIdx" :image="item.otherImage" :size="44" />

                <div class="min-w-0 flex-1">
                    <div class="flex items-center justify-between gap-2">
                        <div class="flex items-center gap-1.5 min-w-0">
                            <span class="font-bold text-sm text-slate-800 truncate">{{ item.otherName }}</span>
                            <span class="text-[10px] font-semibold px-1.5 py-px rounded shrink-0"
                                :class="item.otherRole === '멘토' ? 'bg-violet-50 text-violet-600' : 'bg-emerald-50 text-emerald-600'">
                                {{ item.otherRole }}
                            </span>
                        </div>
                        <span class="text-[10px] text-slate-400 whitespace-nowrap shrink-0">{{ item.time }}</span>
                    </div>

                    <p class="text-[13px] font-semibold text-slate-700 truncate mt-1">{{ item.subject }}</p>

                    <div class="flex items-center justify-between gap-2 mt-1">
                        <p class="text-xs text-slate-400 truncate font-light">{{ item.lastMessage }}</p>
                        <span class="flex items-center gap-1.5 shrink-0">
                            <span v-if="item.unread" class="w-2 h-2 rounded-full bg-brand"></span>
                            <span class="text-[9px] font-bold px-1.5 py-0.5 rounded-full"
                                :class="item.status === 'OPEN' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-400'">
                                {{ item.status === 'OPEN' ? '진행중' : '종료' }}
                            </span>
                        </span>
                    </div>
                </div>
            </button>

            <!-- 빈 상태 -->
            <div v-if="filteredSessions.length === 0" class="flex flex-col items-center justify-center text-center px-6 py-16 text-slate-400">
                <div class="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mb-3">
                    <i class="fa-regular fa-comments text-lg text-slate-300"></i>
                </div>
                <p class="text-sm font-medium text-slate-500">
                    {{ keyword ? '검색 결과가 없습니다' : '멘토링 세션이 없습니다' }}
                </p>
            </div>
        </div>
    </section>
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
