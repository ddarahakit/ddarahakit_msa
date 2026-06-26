<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const goCourseList = () => router.push({ name: 'courseList' })

onMounted(() => { document.title = '멘토링 | 따라학잇' })
</script>

<template>
    <section class="pt-36 pb-24 px-6 min-h-[70vh] hero-bg">
        <div class="max-w-3xl mx-auto text-center">
            <span class="px-4 py-1.5 rounded-full bg-blue-50 border border-blue-100 text-brand text-sm font-semibold inline-block mb-6">
                MENTORING
            </span>
            <h1 class="text-3xl md:text-4xl font-bold text-gray-900 mb-4">1:1 멘토링</h1>
            <p class="text-gray-500 leading-relaxed mb-10">
                현직 멘토와 1:1로 질문하고 코드 리뷰·진로 상담을 받는 멘토링 기능을 준비하고 있습니다.
                곧 만나보실 수 있어요.
            </p>
            <div class="inline-flex items-center gap-2 px-5 py-3 rounded-2xl bg-white border border-gray-100 shadow-sm text-gray-600">
                <i class="fa-solid fa-screwdriver-wrench text-brand"></i>
                <span class="font-semibold">준비 중입니다</span>
            </div>
            <div class="mt-10">
                <button @click="goCourseList" class="text-brand font-semibold hover:underline">
                    실제 수업 둘러보기 <i class="fa-solid fa-chevron-right ml-1 text-xs"></i>
                </button>
            </div>
        </div>
    </section>
</template>

<style scoped>
.hero-bg {
    background: radial-gradient(circle at 50% 50%, rgba(20, 188, 237, 0.08) 0%, rgba(255, 255, 255, 0) 70%);
}
</style>
