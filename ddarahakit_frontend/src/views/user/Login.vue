<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import useAuthStore from '@/stores/useAuthStore'
import validateUtil from '@/utils/validateUtil'
import messageUtil from '@/utils/messageUtil'
import api from '@/api/user'

//라우트 정보 객체
const route = useRoute()

//라우터 정보 객체
const router = useRouter()


//저장소 정보 객체
const authStore = useAuthStore()

//소셜 로그인 시작 (게이트웨이 OAuth2 authorization 으로 리다이렉트)
const backend = import.meta.env.VITE_API_BASE_URL
const loginWithSocial = (provider) => {
    window.location.href = `${backend}/oauth2/authorization/${provider}`
}


//폼 정보 객체
const loginForm = ref()

//에러 객체 정보 객체
const loginInputError = reactive({
    email: {
        errorMessage: null,
        isValid: false
    },
    password: {
        errorMessage: null,
        isValid: false
    }
})

const loginResError = reactive({
    toggle: false,
    message: ''
})

//회원가입(개인) 본인확인 정보 객체
const loginInput = reactive({
    email: '', //이메일(아이디)
    password: '', //비밀번호
})


/**
 * 폼 컨트롤 객체
 * 버튼 활성화 및 텍스트를 제어한다.
 */
const formCntrObj = reactive({
    noStyl: { text: 'text-tip error' },
    submitBtn: { disabled: true }
})

/**
 * 이메일(아이디) 유효성 룰
 *
 * 이메일(아이디) 유효성 룰을 정의한다.
 */
const emailRules = [
    (event) => {
        if (event.target.value) {
            if (/.+@.+\..+/.test(event.target.value)) {
                loginInputError['email'].errorMessage = null
                loginInputError.email.isValid = true
                return true
            } else {
                loginInputError['email'].errorMessage = '이메일 형식으로 입력해주세요.'
                loginInputError.email.isValid = false
                return false
            }
        } else {
            loginInputError['email'].errorMessage = '이메일 주소를 입력해주세요.'
            loginInputError.email.isValid = false
            return false
        }
    }
]

/**
 * 비밀번호 유효성 룰
 *
 * 비밀번호 유효성 룰을 정의한다.
 */
const passwordRules = [
    (event) => {
        if (event.target.value) {
            loginInputError['password'].errorMessage = null
            loginInputError.password.isValid = true
            return true
        } else {
            loginInputError['password'].errorMessage = '비밀번호를 입력해주세요.'
            loginInputError.password.isValid = false
            return false
        }
    }
]


/**
 * 비밀번호 입력 이벤트
 *
 * 비밀번호 입력 이벤트 리스너
 * 로그인 버튼 활성화를 제어한다.
 */
const passwordCheckRules = () => {
    if (loginInput.password.length >= 8) {
        loginInputError.password.isValid = true
    } else {
        loginInputError.password.isValid = false
    }
}


/**
 * 이메일(아이디) 및 도메인명 입력 이벤트
 *
 * 이메일(아이디) 및 도메인명 입력 이벤트 리스너
 *
 * @params type이 em이면 이메일(아이디), dns이면 도메인명
 */
const lowerCase = (type, value) => {
    if (type === 'email') {
        loginInput.email = value.toLowerCase()
        if (loginInput.email.length > 30) {
            loginInput.email = loginInput.email.slice(0, 30)
        }
        return false
    } else {
        loginInput.dns = value.toLowerCase()
        if (loginInput.dns.length > 30) {
            loginInput.dns = loginInput.dns.slice(0, 30)
        }
        return false
    }
}

/**
 * 폼 유효성 검사
 */
const isFormValid = computed(() =>
    loginInputError.email.isValid &&
    loginInputError.password.isValid
);

/**
 * 폼 서브밋
 *
 * 저장소 객체에 존재하는 키에 대응하는 값을 본인확인 객체 값으로 변경한다.
 * 서비스이용동의 화면으로 이동한다.
 */
const submitForm = async () => {
    if (!validateUtil.password(loginInput.password)) {
        loginInputError['password'].errorMessage = messageUtil.getMessage('COM00003')
        return false
    }

    //API: 로그인
    const data = await api.userLogin(loginInput)

    //응답 객체가 아예 없으면(완전한 네트워크 실패 등) 서비스 불가로 안내
    if (!data) {
        loginResError.message = '일시적으로 로그인 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'
        loginResError.toggle = true
        return false
    }

    if (data.success) {
        //로그인
        authStore.login(data.results)

        //코스 목록 페이지 이동
        const redirectPath = route.query.redirect || '/course/list';

        router.push({ path: redirectPath });
        return false
    }

    // === 실패: 버튼만 누르고 무반응처럼 보이지 않도록 항상 화면에 메시지(loginResError) 노출 ===
    // 게이트웨이/네트워크 장애(503·연결 실패 등)는 응답 본문에 code 가 없어 status/code 로 판별한다.
    const isServiceDown =
        data.status === 503 ||
        data.code === 503 ||
        data.code === 'ERR_NETWORK' ||
        data.code === 'ECONNABORTED' ||
        data.code === 'ECONNREFUSED'

    if (data.code === 20004 || data.code === 20005) {
        //이메일 또는 비밀번호 불일치
        loginResError.message = data.message
    } else if (isServiceDown) {
        //서비스 일시 불가 / 네트워크 오류
        loginResError.message = '일시적으로 로그인 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'
    } else {
        //그 외 모든 실패도 조용히 넘기지 않고 일반 메시지 노출
        loginResError.message = data.message || data.errorMessage || '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
    }

    loginResError.toggle = true
    return false
}

//소셜 로그인 실패로 돌아온 경우(?error=oauth2) 안내 노출
onMounted(() => {
    if (route.query.error === 'oauth2') {
        loginResError.message = '소셜 로그인에 실패했습니다. 다시 시도해 주세요.'
        loginResError.toggle = true
    }
})

</script>

<template>
    <main class="pt-24 max-w-7xl mx-auto px-6 py-12 flex flex-col md:flex-row items-center justify-center gap-8">
        <div
            class="max-w-md w-full bg-white rounded-3xl shadow-xl shadow-blue-100/50 border border-gray-100 p-8 md:p-10">
            <div class="text-center mb-10">
                <h1 class="text-3xl font-bold text-gray-900 mb-3">반가워요! 👋</h1>
                <p class="text-gray-500">당신의 성장을 위한 스트리밍을 시작하세요.</p>
            </div>

            <!-- 이메일 로그인 폼 -->
            <div class="space-y-5">
                <v-form ref="loginForm" autocomplete="off" class="space-y-5 comment_form" fast-fail validate-on="blur lazy"
                    @keypress.enter.prevent @submit.prevent="submitForm">
                    <p v-if="loginResError.toggle" class="error-message" style="display: flex;">
                        <i :class="formCntrObj['noStyl'].text" class="fa-solid fa-circle-exclamation"></i>
                        {{ loginResError.message }}
                    </p>

                    <div>
                        <label for="email" class="block text-sm font-semibold text-gray-700 mb-2">이메일</label>
                        <input type="email" id="email" placeholder="example@따라학IT.com"
                            class="w-full px-4 py-3.5 rounded-xl border border-gray-200 focus-ring transition-all placeholder:text-gray-300"
                            tabindex="1" aria-label="이메일(아이디)" required v-model="loginInput.email" @blur="emailRules"
                            :class="{ error: loginInputError.email.errorMessage }"
                            @input="[lowerCase('email', loginInput.email)]" />
                        <p :class="formCntrObj['noStyl'].text" class="error-message">
                            <i class="fa-solid fa-circle-exclamation"></i> {{ loginInputError['email']?.errorMessage }}
                        </p>
                    </div>
                    <div>
                        <div class="flex justify-between mb-2">
                            <label for="password" class="block text-sm font-semibold text-gray-700">비밀번호</label>
                            <RouterLink :to="{ name: 'findPassowrd' }" class="link-btn text-xs text-brand font-medium">비밀번호를 잊으셨나요?</RouterLink>
                        </div>
                        <input required type="password" id="password" placeholder="••••••••"
                            class="w-full px-4 py-3.5 rounded-xl border border-gray-200 focus-ring transition-all placeholder:text-gray-300"
                            tabindex="2" aria-label="비밀번호" v-model="loginInput.password"
                            :class="{ error: loginInputError.password.errorMessage }" @blur="passwordRules"
                            @input="passwordCheckRules" @keypress.enter="submitForm" />
                        <p v-if="loginInputError.password.errorMessage" class="error-message">
                            <i :class="formCntrObj['noStyl'].text" class="fa-solid fa-circle-exclamation"></i>
                            {{ loginInputError['password']?.errorMessage }}
                        </p>
                    </div>

                    <button :disabled="!isFormValid" id="submitBtn" type="submit" tabindex="3"
                        class="submit-btn w-full py-4 bg-brand text-white rounded-xl font-bold text-lg shadow-lg shadow-blue-200 mt-2">
                        로그인
                    </button>
                </v-form>
            </div>

            <!-- 구분선 -->
            <div class="relative my-8 text-center">
                <div class="absolute inset-0 flex items-center">
                    <div class="w-full border-t border-gray-100"></div>
                </div>
                <span class="relative px-4 bg-white text-gray-400 text-sm">또는 간편 로그인</span>
            </div>

            <!-- 소셜 로그인 버튼 -->
            <div class="space-y-3">
                <button type="button" class="social-btn btn-kakao" @click="loginWithSocial('kakao')">
                    <i class="fa-solid fa-comment text-lg"></i> 카카오로 시작하기
                </button>
                <button type="button" class="social-btn btn-google" @click="loginWithSocial('google')">
                    <i class="fa-brands fa-google text-xl"></i>
                    Google로 시작하기
                </button>
            </div>

            <!-- 회원가입 유도 -->
            <div class="mt-10 text-center">
                <p class="text-gray-500 text-sm">
                    아직 계정이 없으신가요?
                    <RouterLink :to="{ name: 'signup' }" class="link-btn text-brand font-bold ml-2">회원가입</RouterLink>
                </p>
            </div>
        </div>
    </main>
</template>

<style scoped>
/* ===== 제출 버튼 (로그인) ===== */
.submit-btn {
    cursor: pointer;
    transition: transform 0.12s ease, box-shadow 0.15s ease,
        background-color 0.15s ease, opacity 0.15s ease;
}

.submit-btn:hover:not(:disabled) {
    transform: translateY(-1px);
    background-color: var(--color-brand-dark, #0ea5e9);
    box-shadow: 0 10px 18px -6px rgba(20, 188, 237, 0.55);
}

.submit-btn:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: 0 3px 8px -4px rgba(20, 188, 237, 0.5);
}

.submit-btn:focus-visible {
    outline: 3px solid rgba(20, 188, 237, 0.5);
    outline-offset: 2px;
}

/* ===== 소셜 버튼 ===== */
.social-btn {
    cursor: pointer;
    transition: transform 0.12s ease, box-shadow 0.15s ease,
        background-color 0.15s ease, border-color 0.15s ease, opacity 0.15s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    width: 100%;
    padding: 12px;
    border-radius: 12px;
    font-weight: 600;
    font-size: 0.95rem;
    border: 1px solid #e5e7eb;
    margin-bottom: 10px;
}

.social-btn:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 14px -4px rgba(0, 0, 0, 0.18);
}

.social-btn:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: 0 2px 6px -2px rgba(0, 0, 0, 0.18);
}

.social-btn:focus-visible {
    outline: 3px solid rgba(20, 188, 237, 0.5);
    outline-offset: 2px;
}

/* 소셜 브랜드 컬러링 */
.btn-kakao {
    background-color: #fee500;
    color: #3c1e1e;
    border: none;
}

.btn-kakao:hover:not(:disabled) {
    background-color: #f7d800;
}

.btn-google {
    background-color: #ffffff;
    color: #1f2937;
}

.btn-google:hover:not(:disabled) {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

/* ===== 링크형 보조 버튼 (비밀번호 찾기 / 회원가입 이동) ===== */
.link-btn {
    display: inline-block;
    cursor: pointer;
    border-radius: 6px;
    padding: 2px 4px;
    text-decoration: none;
    transition: background-color 0.15s ease, color 0.15s ease;
}

.link-btn:hover {
    background-color: rgba(20, 188, 237, 0.1);
    text-decoration: none;
}

.link-btn:focus-visible {
    outline: 3px solid rgba(20, 188, 237, 0.5);
    outline-offset: 2px;
}

/* 모션 최소화 선호 사용자: transform 비활성 */
@media (prefers-reduced-motion: reduce) {
    .submit-btn,
    .social-btn {
        transition: background-color 0.15s ease, opacity 0.15s ease;
    }
    .submit-btn:hover:not(:disabled),
    .submit-btn:active:not(:disabled),
    .social-btn:hover:not(:disabled),
    .social-btn:active:not(:disabled) {
        transform: none;
    }
}

/* 입력 검증 스타일 */
.input-group {
    position: relative;
}

/* 에러 상태 */
.input-error {
    border-color: #ef4444 !important;
    background-color: #fef2f2;
}

.error-message {
    color: #ef4444;
    font-size: 0.75rem;
    margin-top: 0.35rem;
    display: none;
    align-items: center;
    gap: 4px;
}

.input-error+.error-message {
    display: flex;
}

/* 성공 상태 */
.input-success {
    border-color: #10b981 !important;
}

input[type='checkbox'] {
    accent-color: #14bced;
}

.glass-nav {
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

/* 버튼 비활성화 스타일 */
#submitBtn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    pointer-events: none;
    box-shadow: none;
}
</style>