<script setup>
import { computed, ref, watch } from 'vue'
import { userImageUrl } from '@/utils/image'
import { initialOf, avatarColor } from '@/utils/mentoring'

const props = defineProps({
  name: { type: String, default: '' },
  idx: { type: [Number, String], default: null },
  image: { type: String, default: '' },
  size: { type: Number, default: 44 },
})

const broken = ref(false)
// 세션이 바뀌면 이미지 실패 상태 초기화
watch(() => props.image, () => { broken.value = false })

// 실제 프로필 이미지가 있을 때만 사용(기본 플레이스홀더/로드 실패는 이니셜로 폴백)
const resolvedImage = computed(() => {
  const v = props.image
  if (!v || broken.value || v.includes('default_user')) return ''
  return userImageUrl(v)
})
</script>

<template>
  <div
    class="rounded-full overflow-hidden shrink-0 flex items-center justify-center text-white font-bold select-none"
    :style="{ width: `${size}px`, height: `${size}px`, backgroundColor: avatarColor(idx ?? name), fontSize: `${Math.round(size * 0.4)}px` }">
    <img v-if="resolvedImage" :src="resolvedImage" :alt="name" class="w-full h-full object-cover" @error="broken = true" />
    <span v-else>{{ initialOf(name) }}</span>
  </div>
</template>
