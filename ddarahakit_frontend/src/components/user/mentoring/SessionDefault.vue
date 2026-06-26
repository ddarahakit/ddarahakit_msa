<script setup></script>

<template>
  <div class="flex-1 flex flex-col min-h-0">
    <div id="screen-section">
      <div id="remote-video-container">
        <div id="remote-placeholder">
          <div class="pulse-icon mb-4 bg-slate-800 p-6 rounded-full">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#374151" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M15 10l5 5-5 5"></path>
              <path d="M4 4v7a4 4 0 0 0 4 4h12"></path>
            </svg>
          </div>
          <p class="text-slate-400 font-medium">상대방의 화면 공유를 기다리고 있습니다...</p>
        </div>
      </div>
      <div id="resize-handle"></div>
    </div>

    <div id="messages-container" class="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar flex flex-col">
      <div id="empty-state" class="flex-1 flex flex-col items-center justify-center text-slate-400">
        <p class="font-bold text-slate-600">멘토링 세션을 선택해 주세요</p>
      </div>
      <div id="chat-messages-inner" class="space-y-6"></div>
    </div>
  </div>
</template>

<style scoped>
#screen-section {
  display: none;
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
