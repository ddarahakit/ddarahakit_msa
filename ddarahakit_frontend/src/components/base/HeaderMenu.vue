<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()


const props = defineProps({
    isHeaderMenuVisible: {
        type: Boolean,
        required: true
    },
    isLogin: {
        type: Boolean,
        required: true
    }
})

const emits = defineEmits(['closeHeaderMenu'])

const closeHeaderMenu = () => {
    emits('closeHeaderMenu')
}


/**
 * 로그아웃
 *
 * 메뉴를 닫고 전용 로그아웃 페이지(/logout)로 이동한다.
 * 실제 로그아웃 처리는 해당 페이지가 수행한다.
 */
const logout = () => {
    closeHeaderMenu()
    router.push({ name: 'logout' })
}

</script>

<template>
    <div :class="{ active: isHeaderMenuVisible }"
        class="menu d-flex flex-column align-items-end justify-content-start text-right menu_mm trans_400">
        <div class="menu_close_container" @click="closeHeaderMenu">
            <div class="menu_close">
                <div></div>
                <div></div>
            </div>
        </div>
        <!-- <div class="search">
            <form action="#" class="header_search_form menu_mm">
                <input type="search" class="search_input menu_mm" placeholder="Search" required="required">
                <button
                    class="header_search_button d-flex flex-column align-items-center justify-content-center menu_mm">
                    <i class="fa fa-search menu_mm" aria-hidden="true"></i>
                </button>
            </form>
        </div> -->
        <nav class="menu_nav">
            <ul class="menu_mm">
                <li class="menu_mm">
                    <RouterLink :to="{ name: 'courseList' }" @click="closeHeaderMenu">실제 수업</RouterLink>
                </li>
                <li class="menu_mm">
                    <RouterLink :to="{ name: 'mentoring' }" @click="closeHeaderMenu">멘토링</RouterLink>
                </li>
                <li class="menu_mm">
                    <RouterLink :to="{ name: 'roadmap' }" @click="closeHeaderMenu">로드맵</RouterLink>
                </li>
                <li class="menu_mm">
                    <RouterLink :to="{ name: 'projectManagement' }" @click="closeHeaderMenu">포트폴리오</RouterLink>
                </li>
                <li class="menu_mm">
                    <RouterLink :to="{ name: 'communityList' }" @click="closeHeaderMenu">커뮤니티</RouterLink>
                </li>
                <template v-if="!isLogin">
                    <li class="menu_mm">
                        <RouterLink :to="{ name: 'login' }" @click="closeHeaderMenu">로그인</RouterLink>
                    </li>
                    <li class="menu_mm">
                        <RouterLink :to="{ name: 'signup' }" @click="closeHeaderMenu">회원가입</RouterLink>
                    </li>
                </template>
                <template v-else>
                    <li class="menu_mm">
                        <RouterLink :to="{ name: 'dashboard' }" @click="closeHeaderMenu">내 강의실</RouterLink>
                    </li>
                    <li class="menu_mm">
                        <a href="javascript:;" @click="logout">로그아웃</a>
                    </li>
                </template>
            </ul>
        </nav>
    </div>
</template>

<style scoped>
.main_nav_container {}

.main_nav,
.search_button,
.shopping_cart {
    display: inline-block;
}

.main_nav li {
    display: inline-block;
    position: relative;
}

.main_nav li:not(:last-child) {
    margin-right: 44px;
}

.main_nav li a {
    font-size: 18px;
    font-weight: 500;
    color: #384158;
    -webkit-transition: all 200ms ease;
    -moz-transition: all 200ms ease;
    -ms-transition: all 200ms ease;
    -o-transition: all 200ms ease;
    transition: all 200ms ease;
}

.main_nav li a:hover,
.main_nav li.active a {
    color: #14bdee;
}

.main_nav li.active::after {
    display: block;
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 100%;
    height: 2px;
    background: #14bdee;
    content: '';
}

.search_button {
    margin-left: 46px;
    cursor: pointer;
}

.shopping_cart {
    margin-left: 23px;
    cursor: pointer;
}

.search_button i,
.shopping_cart i {
    font-size: 18px;
    color: #181818;
    -webkit-transition: all 200ms ease;
    -moz-transition: all 200ms ease;
    -ms-transition: all 200ms ease;
    -o-transition: all 200ms ease;
    transition: all 200ms ease;
}

.search_button:hover i,
.shopping_cart:hover i {
    color: #14bdee;
}

.header_search_form {
    display: block;
    position: relative;
    width: 40%;
}

.header_search_container {
    position: absolute;
    bottom: 0px;
    left: 0px;
    width: 100%;
    background: #14bdee;
    z-index: -1;
    opacity: 0;
    -webkit-transition: all 400ms ease;
    -moz-transition: all 400ms ease;
    -ms-transition: all 400ms ease;
    -o-transition: all 400ms ease;
    transition: all 400ms ease;
}

.header_search_container.active {
    bottom: -73px;
    opacity: 1;
}

.header_search_content {
    width: 100%;
    height: 73px;
}

.search_input {
    width: 100%;
    height: 40px;
    border: none;
    outline: none;
    padding-left: 20px;
}

.header_search_button {
    position: absolute;
    top: 0;
    right: 0;
    width: 40px;
    height: 100%;
    border: none;
    outline: none;
    cursor: pointer;
}

/*********************************
4. Menu
*********************************/

.menu {
    position: fixed;
    top: 3rem;
    right: -400px;
    width: min(400px, 85vw);
    /* 모바일에서 화면을 넘지 않도록(390px 등) 폭 제한 */
    max-width: 100vw;
    height: 100vh;
    background: #FFFFFF;
    z-index: 1100;
    padding-right: 60px;
    padding-top: 87px;
    padding-left: 50px;
}

.menu .logo a {
    color: #000000;
}

.menu.active {
    right: 0;
    height: 100%;
    box-shadow: -5px 0 15px rgba(0, 0, 0, 0.1);
    z-index: 1050;
}

.menu_close_container {
    position: absolute;
    top: 30px;
    right: 60px;
    width: 18px;
    height: 18px;
    transform-origin: center center;
    -webkit-transform: rotate(45deg);
    -moz-transform: rotate(45deg);
    -ms-transform: rotate(45deg);
    -o-transform: rotate(45deg);
    transform: rotate(45deg);
    cursor: pointer;
}

.menu_close_container:hover .menu_close div {
    background: #14bdee;
}

.menu_close {
    width: 100%;
    height: 100%;
    transform-style: preserve-3D;
}

.menu_close div {
    width: 100%;
    height: 2px;
    background: #232323;
    top: 8px;
    -webkit-transition: all 200ms ease;
    -moz-transition: all 200ms ease;
    -ms-transition: all 200ms ease;
    -o-transition: all 200ms ease;
    transition: all 200ms ease;
}

.menu_close div:last-of-type {
    -webkit-transform: rotate(90deg) translateX(-2px);
    -moz-transform: rotate(90deg) translateX(-2px);
    -ms-transform: rotate(90deg) translateX(-2px);
    -o-transform: rotate(90deg) translateX(-2px);
    transform: rotate(90deg) translateX(-2px);
    transform-origin: center;
}

.menu_close:hover div {
    background: #937c6f;
}

.menu .logo {
    margin-bottom: 60px;
}

.menu_nav ul li {
    margin-bottom: 9px;
}

.menu_nav ul li a {
    font-family: 'Roboto', sans-serif;
    font-size: 14px;
    text-transform: uppercase;
    color: rgba(0, 0, 0, 1);
    font-weight: 700;
    letter-spacing: 0.1em;
    -webkit-transition: all 200ms ease;
    -moz-transition: all 200ms ease;
    -ms-transition: all 200ms ease;
    -o-transition: all 200ms ease;
    transition: all 200ms ease;
}

.menu_nav ul li a:hover {
    color: #14bdee;
}

.menu .search {
    width: 100%;
    margin-bottom: 67px;
}

.search {
    display: inline-block;
    width: 400px;
    -webkit-transform: translateY(2px);
    -moz-transform: translateY(2px);
    -ms-transform: translateY(2px);
    -o-transform: translateY(2px);
    transform: translateY(2px);
}

.menu .header_search_form {
    width: 100%;
}

.search form {
    position: relative;
}

.menu .search_input {
    width: 100%;
    height: 40px;
    background: rgba(0, 0, 0, 0.1);
    border-radius: 3px;
    border: none;
    outline: none;
    padding-left: 15px;
    color: rgba(0, 0, 0, 0.5);
}

.menu .search_input::-webkit-input-placeholder {
    font-family: 'Roboto', sans-serif;
    font-size: 14px !important;
    font-weight: 400 !important;
    color: rgba(0, 0, 0, 0.4) !important;
}

.menu .search_input:-moz-placeholder {
    font-family: 'Roboto', sans-serif;
    font-size: 14px !important;
    font-weight: 400 !important;
    color: rgba(0, 0, 0, 0.4) !important;
}

.menu .search_input::-moz-placeholder {
    font-family: 'Roboto', sans-serif;
    font-size: 14px !important;
    font-weight: 400 !important;
    color: rgba(0, 0, 0, 0.4) !important;
}

.menu .search_input:-ms-input-placeholder {
    font-family: 'Roboto', sans-serif;
    font-size: 14px !important;
    font-weight: 400 !important;
    color: rgba(0, 0, 0, 0.4) !important;
}

.menu .search_input::input-placeholder {
    font-family: 'Roboto', sans-serif;
    font-size: 14px !important;
    font-weight: 400 !important;
    color: rgba(0, 0, 0, 0.4) !important;
}
</style>
