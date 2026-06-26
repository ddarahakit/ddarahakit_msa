<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import useAuthStore from '@/stores/useAuthStore'
import api from '@/api/mentoring'
import { useMentoringSocket } from '@/composables/useMentoringSocket'
import MentoringAvatar from '@/components/user/mentoring/MentoringAvatar.vue'
import { getCounterpart, formatSchedule } from '@/utils/mentoring'

const props = defineProps({
  sessionId: {
    type: [String, Number],
    required: true
  }
})

const authStore = useAuthStore()
const myIdx = authStore.getUserIdx()

const messages = ref([])
const inputMessage = ref('')
const sessionMeta = ref({
  title: '세션 제목',
  date: '날짜 정보'
})
const sessionInfo = ref({})
const counterpart = computed(() => getCounterpart(sessionInfo.value, myIdx))
const sessionStatus = computed(() => sessionInfo.value.status || 'OPEN')

const localVideoRef = ref(null)
const remoteVideoRef = ref(null)
const screenSectionRef = ref(null)

const hasRemoteStream = ref(false)
const isScreenSharing = ref(false)
const screenHeight = ref(300)
const isResizing = ref(false)

let peerConnection = null
let localStream = null
let localVideoSender = null
let pendingIceCandidates = []
let unsubscribeSocket = null

const wsProtocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://'
const wsUrl = import.meta.env.VITE_MENTORING_WS_URL || `${wsProtocol}${window.location.host}/ws`
const { send, onMessage } = useMentoringSocket({ url: wsUrl })

const screenVisible = computed(() => isScreenSharing.value || hasRemoteStream.value)

const rtcConfig = {
  iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
}

const currentUserName = computed(() => authStore.getUserName())

const normalizeIncoming = (raw) => {
  if (!raw) return null
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch (_) {
      return null
    }
  }
  if (raw.payload) {
    try {
      return typeof raw.payload === 'string' ? JSON.parse(raw.payload) : raw.payload
    } catch (_) {
      return raw
    }
  }
  return raw
}

const appendMessage = (message) => {
  messages.value.push(message)
  nextTick(() => {
    const container = document.getElementById('messages-container')
    if (container) container.scrollTop = container.scrollHeight
  })
}

const resetPeerConnection = () => {
  if (peerConnection) {
    try { peerConnection.close() } catch (_) {}
  }

  peerConnection = new RTCPeerConnection(rtcConfig)
  pendingIceCandidates = []

  peerConnection.onicecandidate = (event) => {
    if (!event.candidate) return
    send({
      type: 'ice-candidate',
      sessionId: String(props.sessionId),
      candidate: event.candidate
    })
  }

  peerConnection.ontrack = (event) => {
    hasRemoteStream.value = true
    if (remoteVideoRef.value) {
      remoteVideoRef.value.srcObject = event.streams[0]
      remoteVideoRef.value.play().catch(() => {})
    }
  }
}

const flushPendingIce = async () => {
  if (!peerConnection || !peerConnection.remoteDescription) return
  for (const candidate of pendingIceCandidates) {
    try {
      await peerConnection.addIceCandidate(candidate)
    } catch (_) {}
  }
  pendingIceCandidates = []
}

const joinSession = () => {
  send({
    type: 'join-session',
    sessionId: String(props.sessionId)
  })
}

// 발신자 이름 정규화: 백엔드는 sender 를 {idx,name,profileImageUrl} 객체로 준다.
const resolveSenderName = (item) => {
  if (typeof item.from === 'string') return item.from
  if (item.sender && typeof item.sender === 'object') return item.sender.name || ''
  if (typeof item.sender === 'string') return item.sender
  return item.senderName || item.userName || ''
}

const mapMessage = (item) => {
  const senderIdx = item.sender?.idx ?? item.senderIdx ?? null
  return {
    from: resolveSenderName(item),
    fromIdx: senderIdx,
    message: item.message || item.text || '',
    time: item.time || item.createdAt || '',
    mine: myIdx != null && senderIdx != null && Number(senderIdx) === Number(myIdx),
  }
}

const loadSessionDetail = async () => {
  const res = await api.detail(props.sessionId)
  const result = res?.results || res?.result || {}

  // 과거 호환: 배열로 바로 오는 경우
  if (Array.isArray(result)) {
    messages.value = result.map(mapMessage)
    return
  }

  // 현재 백엔드 구조: { session: {...}, messages: { list: [...] } }
  const session = result.session || result
  const rawList = Array.isArray(result.messages?.list)
    ? result.messages.list
    : Array.isArray(result.messages)
      ? result.messages
      : Array.isArray(result.list)
        ? result.list
        : []

  // 목록은 최신순(DESC)으로 오므로, 채팅은 오래된→최신 순서가 되도록 뒤집는다.
  messages.value = rawList.map(mapMessage).reverse()

  sessionInfo.value = session
  sessionMeta.value = {
    title: session.subject || session.title || '멘토링 세션',
    date: formatSchedule(session.scheduledAt || session.sessionDate || session.date)
  }
}

const makeOffer = async () => {
  const offer = await peerConnection.createOffer({
    offerToReceiveAudio: false,
    offerToReceiveVideo: true
  })
  await peerConnection.setLocalDescription(offer)
  send({
    type: 'offer',
    sessionId: String(props.sessionId),
    offer
  })
}

const startScreenShare = async () => {
  try {
    const stream = await navigator.mediaDevices.getDisplayMedia({
      video: true,
      audio: false
    })

    const track = stream.getVideoTracks()[0]
    localStream = stream
    isScreenSharing.value = true

    if (localVideoRef.value) {
      localVideoRef.value.srcObject = stream
      localVideoRef.value.play().catch(() => {})
    }

    if (localVideoSender) {
      await localVideoSender.replaceTrack(track)
    } else {
      localVideoSender = peerConnection.addTrack(track, stream)
    }

    track.onended = () => {
      stopScreenShare(true)
    }

    await makeOffer()
  } catch (error) {
    console.error('screen share error', error)
  }
}

const stopScreenShare = (notifyOther = false) => {
  if (localStream) {
    localStream.getTracks().forEach((track) => track.stop())
    localStream = null
  }
  isScreenSharing.value = false

  if (localVideoRef.value) {
    localVideoRef.value.srcObject = null
  }

  if (notifyOther) {
    send({
      type: 'share-stopped',
      sessionId: String(props.sessionId)
    })
  }

  resetPeerConnection()
}

const handleSocketMessage = async (payload) => {
  const data = normalizeIncoming(payload)
  if (!data || String(data.sessionId) !== String(props.sessionId)) return

  if (data.type === 'chat-message' || data.type === 'chat') {
    appendMessage({
      from: data.from || data.sender || '',
      fromIdx: data.fromIdx ?? null,
      message: data.message || data.text || '',
      time: data.time || '',
      mine: false
    })
    return
  }

  if (data.type === 'offer' && data.offer) {
    await peerConnection.setRemoteDescription(new RTCSessionDescription(data.offer))
    const answer = await peerConnection.createAnswer()
    await peerConnection.setLocalDescription(answer)
    send({
      type: 'answer',
      sessionId: String(props.sessionId),
      answer
    })
    await flushPendingIce()
    return
  }

  if (data.type === 'answer' && data.answer) {
    await peerConnection.setRemoteDescription(new RTCSessionDescription(data.answer))
    await flushPendingIce()
    return
  }

  if (data.type === 'ice-candidate' && data.candidate) {
    const candidate = new RTCIceCandidate(data.candidate)
    if (!peerConnection.remoteDescription) {
      pendingIceCandidates.push(candidate)
    } else {
      await peerConnection.addIceCandidate(candidate)
    }
    return
  }

  if (data.type === 'share-stopped') {
    hasRemoteStream.value = false
    if (remoteVideoRef.value) {
      remoteVideoRef.value.srcObject = null
    }
  }
}

const sendChat = async () => {
  const text = inputMessage.value.trim()
  if (!text) return

  const message = {
    type: 'chat-message',
    sessionId: String(props.sessionId),
    from: currentUserName.value,
    fromIdx: myIdx,
    message: text,
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false }),
    mine: true
  }

  appendMessage(message)
  inputMessage.value = ''
  send(message)

  // 서버 저장 API가 준비되어 있으면 함께 저장
  if (api.sendMessage) {
    await api.sendMessage(props.sessionId, { message: text })
  }
}

const startResize = () => {
  isResizing.value = true
}

const onResize = (event) => {
  if (!isResizing.value || !screenSectionRef.value) return
  const top = screenSectionRef.value.getBoundingClientRect().top
  const next = event.clientY - top
  if (next >= 150 && next <= window.innerHeight * 0.7) {
    screenHeight.value = next
  }
}

const stopResize = () => {
  isResizing.value = false
}

onMounted(async () => {
  resetPeerConnection()
  await loadSessionDetail()
  joinSession()

  unsubscribeSocket = onMessage(handleSocketMessage)
  window.addEventListener('mousemove', onResize)
  window.addEventListener('mouseup', stopResize)
})

watch(
  () => props.sessionId,
  async () => {
    messages.value = []
    hasRemoteStream.value = false
    resetPeerConnection()
    await loadSessionDetail()
    joinSession()
  }
)

onUnmounted(() => {
  window.removeEventListener('mousemove', onResize)
  window.removeEventListener('mouseup', stopResize)
  if (unsubscribeSocket) unsubscribeSocket()
  stopScreenShare(false)
  if (peerConnection) {
    try { peerConnection.close() } catch (_) {}
  }
})
</script>

<template>
  <div class="flex-1 flex flex-col min-h-0">
    <div
      id="screen-section"
      ref="screenSectionRef"
      :class="{ active: screenVisible }"
      :style="{ height: `${screenHeight}px` }">
      <div id="remote-video-container">
        <div v-if="!hasRemoteStream" id="remote-placeholder">
          <div class="pulse-icon mb-4 bg-slate-800 p-6 rounded-full">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#374151" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M15 10l5 5-5 5"></path>
              <path d="M4 4v7a4 4 0 0 0 4 4h12"></path>
            </svg>
          </div>
          <p class="text-slate-400 font-medium">상대방의 화면 공유를 기다리고 있습니다...</p>
        </div>

        <video ref="remoteVideoRef" id="remote-video" autoplay playsinline :class="{ hidden: !hasRemoteStream }"></video>

        <div id="local-video-container" :style="{ display: isScreenSharing ? 'block' : 'none' }">
          <video ref="localVideoRef" id="local-video" autoplay playsinline muted></video>
          <div class="absolute bottom-2 left-2 bg-black/60 text-[9px] text-white px-1.5 py-0.5 rounded">
            나의 공유 화면
          </div>
        </div>

        <div class="absolute top-4 right-4 flex gap-2 z-50">
          <button
            id="btn-stop-share"
            type="button"
            @click="stopScreenShare(true)"
            class="bg-red-500 hover:bg-red-600 text-white p-2.5 rounded-xl shadow-lg transition-all"
            title="내 공유 중지">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18"></path>
              <path d="m6 6 12 12"></path>
            </svg>
          </button>
        </div>
      </div>
      <div id="resize-handle" @mousedown="startResize"></div>
    </div>

    <div id="chat-header" class="bg-white border-b border-slate-200 p-4 flex items-center justify-between gap-3">
      <div class="flex items-center gap-3 min-w-0">
        <MentoringAvatar
          :name="counterpart.other?.name"
          :idx="counterpart.other?.idx"
          :image="counterpart.other?.profileImageUrl"
          :size="42" />
        <div class="min-w-0">
          <div class="flex items-center gap-1.5">
            <h3 id="active-session-title" class="font-bold text-sm text-slate-800 truncate">
              {{ counterpart.other?.name || '멘토링' }}
            </h3>
            <span class="text-[10px] font-semibold px-1.5 py-px rounded shrink-0"
              :class="counterpart.otherRole === '멘토' ? 'bg-violet-50 text-violet-600' : 'bg-emerald-50 text-emerald-600'">
              {{ counterpart.otherRole }}
            </span>
            <span class="text-[9px] font-bold px-1.5 py-0.5 rounded-full shrink-0"
              :class="sessionStatus === 'OPEN' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-400'">
              {{ sessionStatus === 'OPEN' ? '진행중' : '종료' }}
            </span>
          </div>
          <p id="active-session-date" class="text-[11px] text-slate-400 truncate">{{ sessionMeta.title }}</p>
        </div>
      </div>

      <button
        id="btn-screen-share"
        type="button"
        @click="startScreenShare"
        class="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-bold transition-all shadow-sm">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect width="18" height="14" x="3" y="5" rx="2" ry="2"></rect>
          <path d="M17 21v-2"></path>
          <path d="M7 21v-2"></path>
          <path d="M9 21h6"></path>
        </svg>
        {{ isScreenSharing ? '공유 중' : '화면 공유 시작' }}
      </button>
    </div>

    <div id="messages-container" class="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar flex flex-col">
      <div
        v-for="(item, index) in messages"
        :key="`${index}-${item.time || ''}`"
        :class="item.mine ? 'flex-row-reverse' : 'flex-row'"
        class="flex items-end gap-2 w-full">
        <MentoringAvatar
          v-if="!item.mine"
          :name="item.from"
          :idx="item.fromIdx"
          :size="28"
          class="mb-4" />
        <div
          :class="item.mine ? 'chat-bubble-me' : 'chat-bubble-mentor'"
          class="max-w-[70%] p-3 text-sm shadow-sm rounded-2xl">
          {{ item.message }}
        </div>
        <span class="text-[9px] text-slate-400 mb-1 shrink-0">{{ item.time || '' }}</span>
      </div>
    </div>

    <div id="chat-input-area" class="p-4 bg-white border-t border-slate-100">
      <div id="message-form" class="flex items-center gap-3 bg-slate-50 p-2 rounded-2xl border border-slate-200">
        <input
          @keyup.enter="sendChat"
          v-model="inputMessage"
          type="text"
          id="user-input"
          placeholder="메시지를 입력하세요..."
          class="flex-1 bg-transparent border-none focus:ring-0 text-sm px-4 outline-none" />
        <button
          @click="sendChat"
          type="button"
          class="bg-[#14BCED] text-white px-5 py-2 rounded-xl font-bold text-sm">
          전송
        </button>
      </div>
    </div>
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

.chat-bubble-me {
  background-color: #14bced;
  color: white;
  border-bottom-right-radius: 2px;
}

.chat-bubble-mentor {
  background-color: white;
  color: #1e293b;
  border-bottom-left-radius: 2px;
  border: 1px solid #e2e8f0;
}

#screen-section {
  display: none;
  background: #0f172a;
  position: relative;
  width: 100%;
  overflow: hidden;
  transition: height 0.3s ease;
}

#screen-section.active {
  display: flex;
  flex-direction: column;
}

#remote-video-container {
  flex: 1;
  width: 100%;
  height: 100%;
  position: relative;
  background: #111827;
  display: flex;
  align-items: center;
  justify-content: center;
}

#remote-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  z-index: 10;
}

#remote-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  z-index: 5;
  background: radial-gradient(circle, #1f2937 0%, #111827 100%);
}

#local-video-container {
  display: none;
  position: absolute;
  bottom: 16px;
  right: 16px;
  width: 200px;
  aspect-ratio: 16/9;
  background: #1e293b;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid rgba(20, 188, 237, 0.5);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.4);
  z-index: 40;
}

#local-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

#resize-handle {
  height: 6px;
  background: #334155;
  cursor: ns-resize;
  width: 100%;
  z-index: 50;
}

#resize-handle:hover {
  background: #14bced;
}

.pulse-icon {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }

  50% {
    opacity: 0.5;
    transform: scale(1.05);
  }
}
</style>
