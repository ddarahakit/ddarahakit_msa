<script setup>
import { computed, ref } from 'vue'

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

const getSessionId = (session) => {
    return session.id
        || session.sessionId
        || session.session_id
        || session.idx
        || session.mentoringIdx
        || session.mentoring_id
        || session.chatRoomId
}

const filteredSessions = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    if (!q) return props.sessions
    return props.sessions.filter((session) => {
        const subject = String(session.subject || session.title || '').toLowerCase()
        const lastMessage = String(session.lastMessage || '').toLowerCase()
        return subject.includes(q) || lastMessage.includes(q)
    })
})

const onSelect = (session) => {
    emits('select', session)
}
</script>

<template>
    <section class="w-full lg:w-80 bg-white border-r border-slate-200 flex flex-col shrink-0">
        <div class="p-6 border-b border-slate-100">
            <div class="flex items-center gap-3 mb-6">
                <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" class="w-12 h-12 rounded-full" alt="mentor">
                <div>
                    <h2 class="font-bold text-base">김철수 멘토</h2>
                    <p class="text-[11px] text-slate-400">Senior Developer</p>
                </div>
            </div>
            <input
                v-model="keyword"
                type="text"
                placeholder="세션 검색..."
                class="w-full px-4 py-2 bg-slate-100 border-none rounded-xl text-sm outline-none">
        </div>

        <div id="session-list" class="flex-1 overflow-y-auto custom-scrollbar">
            <button
                type="button"
                @click="onSelect(session)"
                v-for="(session, index) in filteredSessions"
                :key="String(getSessionId(session) || `session-${index}`)"
                class="w-full text-left flex flex-col gap-1 p-5 cursor-pointer hover:bg-slate-50 transition-all border-b border-slate-50"
                :class="String(getSessionId(session)) === String(selectedSessionId) ? 'bg-blue-50/50 border-r-4 border-r-brand' : ''">
                <div class="flex justify-between items-start">
                    <h4 class="font-bold text-sm text-slate-800">{{ session.subject || session.title || '멘토링 세션' }}</h4>
                    <span class="text-[10px] text-slate-400 font-medium whitespace-nowrap ml-2">
                        {{ session.lastMessageTime || session.updatedAt || '' }}
                    </span>
                </div>
                <p class="text-xs text-slate-500 line-clamp-1 mt-1 font-light italic">
                    {{ session.lastMessage || '대화 내역이 없습니다.' }}
                </p>
                <div class="flex items-center gap-2 mt-3">
                    <div class="w-5 h-5 rounded-full bg-blue-100 border border-white flex items-center justify-center">
                        <i class="fa-regular fa-comment text-[10px] text-blue-500"></i>
                    </div>
                    <span class="text-[10px] text-slate-400">{{ session.messageCount || 0 }}개의 대화</span>
                </div>
            </button>
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
