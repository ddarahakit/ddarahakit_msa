<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/course'
import statsApi from '@/api/stats'
import { formatPrice } from '@/utils/price'

const router = useRouter()

// 인기 강의 목록 (백엔드 /course/list 결과에서 상위 N개)
const POPULAR_LIMIT = 4
const popularCourses = ref([])
const isLoading = ref(true)
const isError = ref(false)

// 서비스 통계 (GET /stats/overview)
const stats = ref(null)

/**
 * 큰 수를 약식 한글 단위로 포맷 (예: 123456 → "12만+", 1200 → "1천+")
 */
const formatCount = (n) => {
    const num = Number(n || 0)
    if (num >= 10000) return `${Math.floor(num / 10000)}만+`
    if (num >= 1000) return `${Math.floor(num / 1000)}천+`
    if (num <= 0) return '0'
    return `${num}+`
}

/**
 * 서비스 통계 조회
 */
const getStats = async () => {
    const data = await statsApi.overview()
    if (data && data.success && data.results) {
        stats.value = data.results
    }
}

/**
 * 코스 평균 평점 (CourseList 와 동일 계산식)
 */
const getAverageRating = (course) => {
    if (!course.totalReviewsCount) return '0.0'
    return (
        Math.ceil(
            (course.rating5 * 5 + course.rating4 * 4 + course.rating3 * 3 + course.rating2 * 2 + course.rating1) /
            course.totalReviewsCount * 10
        ) / 10
    ).toFixed(1)
}

// 코스 카드 라벨: 가장 구체적인(leaf) 카테고리명 (CourseList 와 동일)
const courseCategoryName = (course) =>
    course.category?.[course.category.length - 1]?.name || '강의'

/**
 * 인기 강의 목록 조회 (총 주문 수 기준 상위 N개)
 */
const getPopularCourses = async () => {
    isLoading.value = true
    isError.value = false

    const data = await api.courseList()

    if (data && data.success && data.results && Array.isArray(data.results.courses)) {
        const list = [...data.results.courses]
        // 인기순(총 주문 수) 정렬 후 상위 N개
        list.sort((a, b) => (b.totalOrderedCount || 0) - (a.totalOrderedCount || 0))
        popularCourses.value = list.slice(0, POPULAR_LIMIT)
    } else {
        isError.value = true
        popularCourses.value = []
    }

    isLoading.value = false
}

// 강의 상세 이동
const goCourse = (courseIdx) => router.push(`/course/${courseIdx}`)

// 강의 목록 이동
const goCourseList = () => router.push({ name: 'courseList' })

// 로드맵 이동
const goRoadmap = () => router.push({ name: 'roadmap' })

// 회원가입 이동 (CTA)
const goSignup = () => router.push({ name: 'signup' })

onMounted(() => {
    getPopularCourses()
    getStats()
})
</script>

<template>
    <section class="relative pt-40 pb-24 px-6 hero-bg">
        <div class="max-w-7xl mx-auto text-center">
            <span
                class="px-4 py-1.5 rounded-full bg-blue-50 border border-blue-100 text-brand text-sm font-semibold mb-6 inline-block">
                🚀 2026 얼리버드 수강신청 오픈
            </span>
            <h1 class="text-4xl md:text-7xl font-extrabold mb-8 leading-tight text-gray-900">
                배움의 끝이 없는 곳,<br />
                <span class="gradient-text">당신의 커리어를 스트리밍하세요.</span>
            </h1>
            <p class="text-gray-500 text-lg md:text-xl max-w-2xl mx-auto mb-10 leading-relaxed">
                업계 최고 전문가들의 실무 중심 강의부터 1:1 멘토링까지, 따라학IT과 함께라면 당신의 목표는
                이미 현실입니다.
            </p>
            <div class="flex flex-col md:flex-row justify-center gap-4">
                <button @click="goCourseList"
                    class="px-8 py-4 bg-brand text-white rounded-xl font-bold text-lg hover:opacity-90 transition-all shadow-xl shadow-blue-100">
                    지금 바로 수강하기
                </button>
                <button @click="goRoadmap"
                    class="px-8 py-4 bg-white border border-gray-200 text-gray-700 rounded-xl font-bold text-lg hover:border-brand hover:text-brand transition-all">
                    커리큘럼 보러가기
                </button>
            </div>

            <!-- 대시보드 미리보기 이미지 영역 -->
            <div class="mt-20 relative max-w-5xl mx-auto">
                <div class="absolute -inset-1 bg-gradient-to-r from-[#14BCED] to-[#0ea5e9] rounded-2xl blur opacity-20">
                </div>
                <div class="relative bg-white rounded-2xl border border-gray-100 p-2 overflow-hidden shadow-2xl">
                    <div
                        class="bg-gray-50 rounded-lg aspect-video flex items-center justify-center border border-gray-100">
                        <span class="text-gray-400 flex flex-col items-center">
                            <i class="fa-solid fa-play text-6xl mb-4 text-brand/30"></i>
                            <p>[강의 플랫폼 대시보드 미리보기 이미지]</p>
                        </span>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- 통계 섹션 -->
    <section class="py-16 bg-white border-y border-gray-100">
        <div class="max-w-7xl mx-auto px-6 grid grid-cols-2 md:grid-cols-4 gap-8">
            <div class="text-center">
                <p class="text-3xl md:text-4xl font-bold mb-2 text-gray-900">{{ stats ? formatCount(stats.courseCount) : '–' }}</p>
                <p class="text-gray-400 text-sm">보유 강의 수</p>
            </div>
            <div class="text-center">
                <p class="text-3xl md:text-4xl font-bold mb-2 text-gray-900">{{ stats ? formatCount(stats.studentCount) : '–' }}</p>
                <p class="text-gray-400 text-sm">누적 수강생</p>
            </div>
            <div class="text-center">
                <p class="text-3xl md:text-4xl font-bold mb-2 text-gray-900">{{ stats ? stats.satisfactionRate + '%' : '–' }}</p>
                <p class="text-gray-400 text-sm">강의 만족도</p>
            </div>
            <div class="text-center">
                <p class="text-3xl md:text-4xl font-bold mb-2 text-gray-900">1:1</p>
                <p class="text-gray-400 text-sm">밀착 멘토링</p>
            </div>
        </div>
    </section>

    <!-- 인기 강의 섹션 -->
    <section class="py-24 px-6 bg-gray-50/50">
        <div class="max-w-7xl mx-auto">
            <div class="flex justify-between items-end mb-12">
                <div>
                    <h2 class="text-3xl font-bold mb-4 text-gray-900">현재 가장 핫한 강의 🔥</h2>
                    <p class="text-gray-500">많은 수강생들이 선택한 검증된 교육 과정을 확인해보세요.</p>
                </div>
                <button @click="goCourseList" class="hidden md:block text-brand font-semibold hover:underline">
                    전체보기 <i class="fa-solid fa-chevron-right ml-1"></i>
                </button>
            </div>

            <!-- 로딩 상태 (스켈레톤) -->
            <div v-if="isLoading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                <div v-for="n in POPULAR_LIMIT" :key="n"
                    class="bg-white rounded-2xl border border-gray-100 overflow-hidden shadow-sm animate-pulse">
                    <div class="h-40 bg-gray-100"></div>
                    <div class="p-5 space-y-3">
                        <div class="h-3 w-20 bg-gray-100 rounded"></div>
                        <div class="h-5 w-3/4 bg-gray-100 rounded"></div>
                        <div class="h-10 bg-gray-100 rounded"></div>
                    </div>
                </div>
            </div>

            <!-- 에러 상태 -->
            <div v-else-if="isError" class="text-center py-16 text-gray-400">
                <i class="fa-solid fa-triangle-exclamation text-4xl mb-4"></i>
                <p>강의를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.</p>
            </div>

            <!-- 빈 상태 -->
            <div v-else-if="popularCourses.length === 0" class="text-center py-16 text-gray-400">
                <i class="fa-solid fa-book-open text-4xl mb-4"></i>
                <p>등록된 강의가 없습니다.</p>
            </div>

            <!-- 실제 데이터 -->
            <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                <div v-for="course in popularCourses" :key="course.idx" @click="goCourse(course.idx)"
                    class="bg-white rounded-2xl border border-gray-100 overflow-hidden card-hover cursor-pointer shadow-sm">
                    <div class="h-40 bg-blue-50 flex items-center justify-center overflow-hidden">
                        <img v-if="course.image" :src="`${course.image}`" :alt="course.name"
                            class="w-full h-full object-cover" />
                        <i v-else class="fa-solid fa-graduation-cap text-6xl text-brand/40"></i>
                    </div>
                    <div class="p-5">
                        <span class="text-[10px] font-bold text-brand mb-2 block uppercase tracking-wider">
                            {{ courseCategoryName(course) }}
                        </span>
                        <h3 class="text-lg font-bold mb-2 text-gray-900 line-clamp-2">
                            {{ course.name }}
                        </h3>
                        <p class="text-gray-500 text-xs mb-4 line-clamp-2">
                            {{ course.description }}
                        </p>
                        <div class="flex items-center justify-between pt-4 border-t border-gray-50">
                            <span class="font-bold text-gray-900 text-md">{{ formatPrice(course.salePrice) }}</span>
                            <div class="flex items-center text-yellow-400 text-xs">
                                <i class="fa-solid fa-star mr-1"></i> {{ getAverageRating(course) }}
                                <span class="text-gray-400 ml-1">({{ course.totalReviewsCount || 0 }})</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- 하단 CTA 섹션 -->
    <section class="py-20 px-6">
        <div
            class="max-w-5xl mx-auto bg-gradient-to-r from-[#14BCED] to-[#0ea5e9] rounded-3xl p-10 md:p-16 text-center overflow-hidden relative shadow-2xl shadow-blue-100">
            <div class="absolute top-0 right-0 -mr-10 -mt-10 w-40 h-40 bg-white/10 rounded-full blur-3xl"></div>
            <div class="relative z-10">
                <h2 class="text-3xl md:text-5xl font-bold mb-6 text-white">
                    지금 가입하고 다양한 강의로 성장하세요
                </h2>
                <p class="text-blue-50 mb-10 text-lg opacity-90">
                    선착순 100명 한정, 평생 소장 강의권도 증정합니다.
                </p>
                <div class="flex flex-col sm:flex-row justify-center gap-4">
                    <input type="email" placeholder="이메일 주소 입력"
                        class="px-6 py-4 rounded-xl bg-white text-gray-900 focus:outline-none sm:w-80 shadow-inner" />
                    <button @click="goSignup"
                        class="px-8 py-4 bg-gray-900 text-white rounded-xl font-bold hover:bg-black transition-all">
                        혜택 받기
                    </button>
                </div>
            </div>
        </div>
    </section>
</template>

<style scoped>
.hero-bg {
    background: radial-gradient(circle at 50% 50%, rgba(20, 188, 237, 0.08) 0%, rgba(255, 255, 255, 0) 70%);
}
</style>