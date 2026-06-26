<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import useAuthStore from '@/stores/useAuthStore'
import api from '@/api/user'
import UserDashboardSidebar from '@/components/user/UserDashboardSidebar.vue'
import UserAvatar from '@/components/base/UserAvatar.vue'

const authStore = useAuthStore()

// 사용자 프로필 (자기소개 등)
const userProfile = ref({ introduction: '' })

const getUserProfile = async () => {
    const data = await api.userProfile()
    if (data.success && data.results) {
        userProfile.value = data.results
        // 헤더가 옛/기본 이미지를 들고 있어도 최신 프로필 이미지로 동기화
        authStore.setUserProfileImage(data.results.profileImageUrl)
    }
}

// 구매 코스 목록
const paidCourseList = ref([])
const COURSE_DISPLAY_STEP = 2
const courseDisplayCount = ref(COURSE_DISPLAY_STEP)
const displayedCourses = computed(() => paidCourseList.value.slice(0, courseDisplayCount.value))

// 탭 활성화 상태
const activeTab = ref('questions')

// 탭별 데이터
const myQuestionList = ref([])
const myPostList = ref([])
const myReviewList = ref([])

// 탭별 로딩 상태
const tabLoading = ref(false)

// 이미 로드된 탭 추적 (캐싱)
const loadedTabs = ref(new Set())

// 탭별 표시 개수
const DISPLAY_STEP = 3
const displayCount = reactive({
    questions: DISPLAY_STEP,
    community: DISPLAY_STEP,
    review: DISPLAY_STEP
})

const displayedQuestions = computed(() => myQuestionList.value.slice(0, displayCount.questions))
const displayedPosts = computed(() => myPostList.value.slice(0, displayCount.community))
const displayedReviews = computed(() => myReviewList.value.slice(0, displayCount.review))

const showMore = (tab) => {
    displayCount[tab] += DISPLAY_STEP
}

/**
 * 구매 코스 목록 조회
 */
const getPaidCourseList = async () => {
    const data = await api.getPaidCourseList()
    if (data.success && data.results) {
        paidCourseList.value = data.results
    }
}

/**
 * 탭 데이터 로드
 */
const loadTabData = async (tab) => {
    if (loadedTabs.value.has(tab)) return

    tabLoading.value = true

    if (tab === 'questions') {
        const data = await api.getMyQuestionList()
        if (data.success && data.results) {
            myQuestionList.value = data.results
        }
    } else if (tab === 'community') {
        const data = await api.getMyPostList()
        if (data.success && data.results) {
            myPostList.value = data.results
        }
    } else if (tab === 'review') {
        const data = await api.getMyReviewList()
        if (data.success && data.results) {
            myReviewList.value = data.results
        }
    }

    loadedTabs.value.add(tab)
    tabLoading.value = false
}

/**
 * 탭 클릭 핸들러
 */
const onTabClick = (tab) => {
    activeTab.value = tab
    displayCount[tab] = DISPLAY_STEP
    loadTabData(tab)
}

/**
 * 강의 진행률 계산
 */
const getProgress = (course) => {
    const totalLectures = course.sections.reduce((sum, section) => sum + section.lectures.length, 0)
    if (totalLectures === 0) return 0
    const completedLectures = course.sections.flatMap(s => s.lectures).filter(l => l.complete).length
    return Math.round(completedLectures / totalLectures * 100)
}

/**
 * 다음 강의명 가져오기
 */
const getNextLectureName = (course) => {
    for (const section of course.sections) {
        for (const lecture of section.lectures) {
            if (!lecture.complete) {
                return lecture.name
            }
        }
    }
    return '모든 강의를 수강했습니다'
}

// ── 주간 학습활동 (GET /user/study/weekly) ─────────────────────────────
const weekly = ref(null)
const weeklyLoading = ref(true)
const weeklyError = ref(false)

const WEEKDAY_KO = ['일', '월', '화', '수', '목', '금', '토']

// 막대 차트용: 일별 count 와 최대값 (스케일링)
const weeklyDays = computed(() => weekly.value?.days || [])
const weeklyMax = computed(() => Math.max(1, ...weeklyDays.value.map(d => Number(d.count) || 0)))
// 이번 주 학습 기록이 전혀 없는지(0건)
const weeklyEmpty = computed(() =>
    weeklyDays.value.length > 0 && weeklyDays.value.every(d => (Number(d.count) || 0) === 0)
)

// 날짜(yyyy-MM-dd) → 요일 라벨
const weekdayLabel = (dateStr) => {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    if (Number.isNaN(d.getTime())) return ''
    return WEEKDAY_KO[d.getDay()]
}
// 오늘 날짜인지 (강조용)
const isToday = (dateStr) => {
    if (!dateStr) return false
    const today = new Date()
    const ymd = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
    return dateStr === ymd
}

/**
 * 주간 학습활동 조회
 */
const getWeeklyStudy = async () => {
    weeklyLoading.value = true
    weeklyError.value = false
    const data = await api.getWeeklyStudy()
    if (data && data.success && data.results) {
        weekly.value = data.results
    } else {
        weeklyError.value = true
        weekly.value = null
    }
    weeklyLoading.value = false
}

onMounted(() => {
    getUserProfile()
    getPaidCourseList()
    loadTabData('questions')
    getWeeklyStudy()
    document.title = '대시보드 | 따라학잇'
})
</script>

<template>
    <main class="max-w-7xl mx-auto px-6 pt-28 pb-20">
        <div class="flex flex-col lg:flex-row gap-12">
            <UserDashboardSidebar />

            <!-- Main Content Area -->
            <div class="flex-grow space-y-12">
                <header class="flex flex-col md:flex-row md:items-center justify-between gap-6">
                    <div>
                        <h1 class="text-3xl font-bold text-slate-900 tracking-tight">
                            좋은 아침입니다, {{ authStore.getUserName() }}님! ☕
                        </h1>
                        <p class="text-slate-500 text-sm mt-1">오늘도 지식을 넓힐 준비가 되셨나요?</p>
                    </div>
                </header>

                <!-- 프로필 카드 -->
                <section class="profile-card rounded-3xl p-6 md:p-10 mb-8 text-white relative overflow-hidden shadow-xl shadow-blue-100">
                    <div class="bg-pattern-icons">
                        <i class="fa-solid fa-laptop text-4xl"></i>
                        <span></span>
                        <span></span>
                        <i class="fa-solid fa-keyboard text-4xl mt-10"></i>
                        <i class="fa-solid fa-mouse text-3xl ml-10"></i>
                        <i class="fa-solid fa-display text-4xl mt-5"></i>
                        <span></span>
                        <span></span>
                        <i class="fa-solid fa-microchip text-3xl"></i>
                        <i class="fa-solid fa-code text-4xl mt-12"></i>
                        <i class="fa-solid fa-server text-3xl ml-5"></i>
                        <i class="fa-solid fa-headphones text-4xl mt-4"></i>
                        <i class="fa-solid fa-mobile-screen text-3xl"></i>
                    </div>
                    <div class="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl -mr-20 -mt-20">
                    </div>
                    <div class="relative z-10 flex flex-col md:flex-row items-center gap-8">
                        <div class="relative">
                            <div
                                class="bg-white w-24 h-24 md:w-32 md:h-32 rounded-3xl overflow-hidden border-4 border-black/20 shadow-xl">
                                <UserAvatar :src="userProfile.profileImageUrl" alt="User Avatar" icon-class="text-5xl" />
                            </div>
                        </div>
                        <div class="flex-grow text-center md:text-left">
                            <h2 class="text-2xl md:text-3xl font-bold mb-2">
                                {{ authStore.getUserName() }}
                            </h2>
                            <p class="text-blue-50 text-sm mb-6 opacity-90">
                                {{ userProfile.introduction || '아직 자기소개가 없습니다.' }}
                            </p>
                            <div class="flex flex-wrap justify-center md:justify-start gap-3">
                                <div class="px-4 py-2 bg-white/20 rounded-xl border border-white/20">
                                    <span
                                        class="text-[10px] text-blue-100 block uppercase font-bold tracking-wider mb-1">연속
                                        학습</span>
                                    <span class="text-lg font-bold">{{ weekly ? weekly.streakDays : 0 }}일째</span>
                                </div>
                                <div class="px-4 py-2 bg-white/20 rounded-xl border border-white/20">
                                    <span
                                        class="text-[10px] text-blue-100 block uppercase font-bold tracking-wider mb-1">보유
                                        강의</span>
                                    <span class="text-lg font-bold">{{ paidCourseList.length }}개</span>
                                </div>
                            </div>
                        </div>
                        <div class="shrink-0">
                            <a href="/user/profile"
                                class="inline-block px-6 py-3 bg-white text-brand hover:bg-blue-50 rounded-xl font-bold text-sm transition-all shadow-lg">
                                <i class="fa-solid fa-user-gear mr-2"></i> 프로필 수정
                            </a>
                        </div>
                    </div>
                </section>

                <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    <!-- 왼쪽: 강의 및 활동 -->
                    <div class="lg:col-span-2">
                        <div class="flex justify-between items-center mb-6">
                            <h3 class="text-xl font-bold flex items-center gap-2 text-slate-800">
                                <i class="fa-solid fa-play text-brand"></i> 수강 중인 강의
                            </h3>
                            <button v-if="paidCourseList.length > COURSE_DISPLAY_STEP"
                                @click="courseDisplayCount = courseDisplayCount >= paidCourseList.length ? COURSE_DISPLAY_STEP : paidCourseList.length"
                                class="text-slate-400 text-xs hover:text-brand transition-colors">
                                {{ courseDisplayCount >= paidCourseList.length ? '접기' : '전체 보기' }}
                            </button>
                        </div>

                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <!-- 수강 중인 강의가 없을 때 -->
                            <div v-if="paidCourseList.length === 0"
                                class="md:col-span-2 text-center py-12 text-slate-400">
                                <i class="fa-solid fa-book-open text-4xl mb-4"></i>
                                <p>아직 수강 중인 강의가 없습니다.</p>
                            </div>

                            <!-- 강의 카드 -->
                            <RouterLink v-for="(course, index) in displayedCourses" :key="index"
                                :to="`/lecture/${course.idx}/${course.nextLectureIdx}`"
                                class="card-shadow rounded-2xl overflow-hidden group cursor-pointer bg-white">
                                <div class="relative h-40 bg-slate-50 overflow-hidden">
                                    <div class="absolute inset-0 flex items-center justify-center">
                                        <img :src="`${course.image}`" :alt="course.name"
                                            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500">
                                    </div>
                                    <div class="absolute bottom-4 left-4">
                                        <span class="text-[10px] font-black text-white bg-brand px-2 py-1 rounded shadow-lg shadow-blue-200 uppercase">학습 중</span>
                                    </div>
                                </div>
                                <div class="p-6">
                                    <h3 class="font-bold text-slate-800 text-lg mb-2 group-hover:text-brand transition-colors line-clamp-1">
                                        {{ course.name }}
                                    </h3>
                                    <p class="text-xs text-slate-400 mb-6">
                                        최근 학습: {{ getNextLectureName(course) }}
                                    </p>
                                    <div class="space-y-2">
                                        <div class="flex justify-between text-[11px] font-bold">
                                            <span class="text-brand">전체 진행률</span>
                                            <span class="text-slate-600">{{ getProgress(course) }}%</span>
                                        </div>
                                        <div class="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                                            <div class="bg-brand h-full rounded-full transition-all duration-500"
                                                :style="`width: ${getProgress(course)}%`">
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </RouterLink>
                        </div>

                        <!-- 탭 섹션 -->
                        <div class="mt-12">
                            <div class="flex border-b border-slate-200 mb-6">
                                <button @click="onTabClick('questions')"
                                    class="px-6 py-3 text-sm font-bold"
                                    :class="activeTab === 'questions' ? 'tab-active' : 'text-slate-400 hover:text-slate-600'">
                                    내가 쓴 질문
                                </button>
                                <button @click="onTabClick('community')"
                                    class="px-6 py-3 text-sm font-bold"
                                    :class="activeTab === 'community' ? 'tab-active' : 'text-slate-400 hover:text-slate-600'">
                                    커뮤니티 게시글
                                </button>
                                <button @click="onTabClick('review')"
                                    class="px-6 py-3 text-sm font-bold"
                                    :class="activeTab === 'review' ? 'tab-active' : 'text-slate-400 hover:text-slate-600'">
                                    강의 리뷰
                                </button>
                            </div>

                            <!-- 로딩 상태 -->
                            <div v-if="tabLoading" class="space-y-3">
                                <div v-for="n in 3" :key="n"
                                    class="white-card rounded-xl p-4 flex gap-4">
                                    <div class="shrink-0 w-10 h-10 bg-slate-100 rounded-full skeleton"></div>
                                    <div class="flex-grow space-y-2">
                                        <div class="w-3/4 h-4 bg-slate-100 rounded skeleton"></div>
                                        <div class="w-1/2 h-3 bg-slate-100 rounded skeleton"></div>
                                    </div>
                                </div>
                            </div>

                            <!-- 내가 쓴 질문 -->
                            <div v-else-if="activeTab === 'questions'" class="space-y-3">
                                <div v-if="myQuestionList.length === 0" class="text-center py-8 text-slate-400">
                                    <p class="text-sm">아직 작성한 질문이 없습니다.</p>
                                </div>
                                <RouterLink v-for="(question, index) in displayedQuestions" :key="index"
                                    :to="`/community/${question.idx}`"
                                    class="white-card rounded-xl p-4 flex gap-4 hover:bg-slate-50 transition-colors cursor-pointer"
                                    :class="question.commentCount > 0 ? 'border-l-4 border-l-brand' : ''">
                                    <div class="shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
                                        :class="question.commentCount > 0 ? 'bg-blue-50 text-brand' : 'bg-slate-50 text-slate-400'">
                                        <i class="fa-regular fa-comment-dots"></i>
                                    </div>
                                    <div class="flex-grow min-w-0">
                                        <div class="flex justify-between items-start mb-1">
                                            <h5 class="text-sm font-bold text-slate-800 line-clamp-1">
                                                {{ question.title }}
                                            </h5>
                                            <span v-if="question.commentCount > 0"
                                                class="text-[10px] text-brand font-bold bg-blue-50 px-2 py-0.5 rounded shrink-0 ml-2">답변 {{ question.commentCount }}개</span>
                                            <span v-else
                                                class="text-[10px] text-slate-400 font-bold bg-slate-100 px-2 py-0.5 rounded shrink-0 ml-2">답변 대기</span>
                                        </div>
                                        <p v-if="question.text" class="text-xs text-slate-500 line-clamp-1 mb-1">{{ question.text }}</p>
                                        <div class="flex items-center gap-3 text-[10px] text-slate-400">
                                            <span class="font-medium">{{ question.courseName || '일반 질문' }}</span>
                                            <span>•</span>
                                            <span>{{ question.updatedAt }}</span>
                                        </div>
                                    </div>
                                </RouterLink>
                                <button v-if="displayCount.questions < myQuestionList.length"
                                    @click="showMore('questions')"
                                    class="w-full py-3 text-sm font-bold text-slate-400 hover:text-brand border border-slate-100 rounded-xl hover:border-brand/30 transition-colors">
                                    더보기 <i class="fa-solid fa-chevron-down ml-1 text-[10px]"></i>
                                </button>
                            </div>

                            <!-- 커뮤니티 게시글 -->
                            <div v-else-if="activeTab === 'community'" class="space-y-3">
                                <div v-if="myPostList.length === 0" class="text-center py-8 text-slate-400">
                                    <p class="text-sm">아직 작성한 게시글이 없습니다.</p>
                                </div>
                                <RouterLink v-for="(post, index) in displayedPosts" :key="index"
                                    :to="`/community/${post.idx}`"
                                    class="white-card rounded-xl p-4 flex gap-4 hover:bg-slate-50 transition-colors cursor-pointer">
                                    <div class="shrink-0 w-10 h-10 bg-slate-50 rounded-full flex items-center justify-center text-slate-400">
                                        <i class="fa-regular fa-pen-to-square"></i>
                                    </div>
                                    <div class="flex-grow min-w-0">
                                        <div class="flex justify-between items-start mb-1">
                                            <h5 class="text-sm font-bold text-slate-800 line-clamp-1">
                                                {{ post.title }}
                                            </h5>
                                            <span class="text-[10px] text-slate-400 font-bold bg-slate-100 px-2 py-0.5 rounded shrink-0 ml-2">
                                                <i class="fa-regular fa-comment-dots mr-1"></i>{{ post.commentCount || 0 }}
                                            </span>
                                        </div>
                                        <p v-if="post.text" class="text-xs text-slate-500 line-clamp-1 mb-1">{{ post.text }}</p>
                                        <div class="flex items-center gap-3 text-[10px] text-slate-400">
                                            <span>{{ post.updatedAt }}</span>
                                        </div>
                                    </div>
                                </RouterLink>
                                <button v-if="displayCount.community < myPostList.length"
                                    @click="showMore('community')"
                                    class="w-full py-3 text-sm font-bold text-slate-400 hover:text-brand border border-slate-100 rounded-xl hover:border-brand/30 transition-colors">
                                    더보기 <i class="fa-solid fa-chevron-down ml-1 text-[10px]"></i>
                                </button>
                            </div>

                            <!-- 강의 리뷰 -->
                            <div v-else-if="activeTab === 'review'" class="space-y-3">
                                <div v-if="myReviewList.length === 0" class="text-center py-8 text-slate-400">
                                    <p class="text-sm">아직 작성한 강의 리뷰가 없습니다.</p>
                                </div>
                                <RouterLink v-for="(item, index) in displayedReviews" :key="index"
                                    :to="`/course/${item.courseIdx}?tab=reviews`"
                                    class="white-card rounded-xl p-4 flex gap-4 hover:bg-slate-50 transition-colors cursor-pointer">
                                    <div class="shrink-0 w-10 h-10 bg-amber-50 rounded-full flex items-center justify-center text-amber-400">
                                        <i class="fa-solid fa-star"></i>
                                    </div>
                                    <div class="flex-grow">
                                        <div class="flex justify-between items-start mb-1">
                                            <h5 class="text-sm font-bold text-slate-800 line-clamp-1">
                                                {{ item.name }}
                                            </h5>
                                            <div class="flex items-center gap-0.5 shrink-0 ml-2">
                                                <i v-for="n in 5" :key="n" class="fa-solid fa-star text-[10px]"
                                                    :class="n <= item.review.rating ? 'text-amber-400' : 'text-slate-200'"></i>
                                            </div>
                                        </div>
                                        <p class="text-xs text-slate-500 line-clamp-1 mb-1">{{ item.review.comment }}</p>
                                    </div>
                                </RouterLink>
                                <button v-if="displayCount.review < myReviewList.length"
                                    @click="showMore('review')"
                                    class="w-full py-3 text-sm font-bold text-slate-400 hover:text-brand border border-slate-100 rounded-xl hover:border-brand/30 transition-colors">
                                    더보기 <i class="fa-solid fa-chevron-down ml-1 text-[10px]"></i>
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- 오른쪽: 사이드바 -->
                    <div class="space-y-6">
                        <!-- 주간 학습 활동 (GET /user/study/weekly) -->
                        <div class="white-card rounded-2xl p-6">
                            <div class="flex items-center justify-between mb-6">
                                <h3 class="text-base font-bold text-slate-800">주간 학습 활동</h3>
                                <span v-if="weekly && weekly.streakDays > 0" class="text-[11px] font-bold text-brand">
                                    🔥 연속 {{ weekly.streakDays }}일째
                                </span>
                            </div>

                            <!-- 로딩 -->
                            <div v-if="weeklyLoading" class="flex items-end justify-between h-32 gap-2 mb-6 px-2 animate-pulse">
                                <div v-for="n in 7" :key="n" class="flex-1 bg-slate-100 rounded-t-md" :style="`height:${20 + (n % 3) * 20}%`"></div>
                            </div>

                            <!-- 에러 -->
                            <div v-else-if="weeklyError" class="h-32 flex items-center justify-center text-xs text-slate-400">
                                학습 활동을 불러오지 못했습니다.
                            </div>

                            <!-- 데이터 (빈 상태 포함) -->
                            <template v-else>
                                <div class="flex items-end justify-between h-32 gap-2 mb-4 px-2">
                                    <div v-for="day in weeklyDays" :key="day.date" class="flex flex-col items-center flex-1 gap-2 h-full justify-end">
                                        <div class="w-full bg-slate-50 rounded-t-md relative" style="height: 100%;">
                                            <div class="absolute bottom-0 inset-x-0 rounded-t-md transition-all"
                                                :class="isToday(day.date) ? 'bg-brand shadow-lg shadow-blue-100' : 'bg-brand/30'"
                                                :style="`height: ${Math.round((Number(day.count) || 0) / weeklyMax * 100)}%`">
                                            </div>
                                        </div>
                                        <span class="text-[10px] font-medium" :class="isToday(day.date) ? 'text-brand font-bold' : 'text-slate-400'">
                                            {{ weekdayLabel(day.date) }}
                                        </span>
                                    </div>
                                </div>

                                <!-- 이번 주 기록이 0건일 때 안내 -->
                                <p v-if="weeklyEmpty" class="text-[11px] text-slate-400 text-center mb-4">
                                    이번 주 학습 기록이 아직 없어요. 강의를 수강해 보세요!
                                </p>

                                <div class="pt-6 border-t border-slate-100">
                                    <div class="flex justify-between items-center mb-2">
                                        <span class="text-[11px] font-bold text-slate-500">
                                            주간 목표 ({{ weekly?.weeklyCompleted ?? 0 }}/{{ weekly?.weeklyGoal ?? 5 }}강)
                                        </span>
                                        <span class="text-xs font-black text-slate-800">{{ weekly?.goalRate ?? 0 }}%</span>
                                    </div>
                                    <div class="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                                        <div class="bg-brand h-full rounded-full transition-all" :style="`width: ${weekly?.goalRate ?? 0}%`"></div>
                                    </div>
                                </div>
                            </template>
                        </div>

                        <!-- 멘토링 진입 카드 -->
                        <RouterLink :to="{ name: 'mentoring' }"
                            class="mentoring-card group block rounded-2xl p-6 text-white relative overflow-hidden shadow-lg shadow-blue-100">
                            <div class="absolute -right-5 -bottom-6 opacity-20 pointer-events-none">
                                <i class="fa-solid fa-comments text-7xl"></i>
                            </div>
                            <div class="relative z-10">
                                <span class="text-[10px] font-bold uppercase tracking-widest text-blue-50/90">Mentoring</span>
                                <h3 class="text-lg font-bold mt-1 mb-1.5">멘토와 1:1 대화</h3>
                                <p class="text-[13px] text-blue-50/90 mb-5 leading-relaxed">
                                    막힌 부분을 멘토에게 직접 물어보고<br />함께 성장하세요.
                                </p>
                                <span class="inline-flex items-center gap-1.5 text-sm font-bold group-hover:gap-2.5 transition-all">
                                    멘토링 바로가기 <i class="fa-solid fa-arrow-right text-xs"></i>
                                </span>
                            </div>
                        </RouterLink>

                    </div>
                </div>
            </div>
        </div>
    </main>
</template>

<style scoped>
.profile-card {
    background: linear-gradient(135deg, #14BCED 0%, #0ea5e9 50%, #0284c7 100%);
}

.mentoring-card {
    background: linear-gradient(135deg, #6366F1 0%, #4f46e5 55%, #4338ca 100%);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.mentoring-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px -8px rgba(79, 70, 229, 0.45);
}

.bg-pattern-icons {
    position: absolute;
    inset: 0;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-around;
    opacity: 0.08;
    pointer-events: none;
    padding: 1rem;
}

.white-card {
    background: white;
    border: 1px solid #f1f5f9;
}

.card-shadow {
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.card-shadow:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.tab-active {
    color: #14BCED;
    border-bottom: 2px solid #14BCED;
}

.line-clamp-1 {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.skeleton {
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
}

@keyframes skeleton-loading {
    0% {
        background-position: 200% 0;
    }
    100% {
        background-position: -200% 0;
    }
}
</style>
