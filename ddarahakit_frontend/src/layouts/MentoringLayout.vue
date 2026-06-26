<script setup>
// 멘토링 전용 최소 레이아웃: 전역 마케팅 헤더(max-w-7xl 여백)·푸터 없이 전체 폭을 사용한다.
// 헤더는 각 페이지(Mentoring.vue)가 강의 수강 페이지처럼 직접 풀폭으로 렌더한다.
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style scoped></style>
