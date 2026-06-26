import { createRouter, createWebHistory } from "vue-router";
import mainRoutes from "./mainRoutes";
import userRoutes from "./userRoutes";
import courseRoutes from "./courseRoutes";
import lectureRoutes from "./lectureRoutes";
import communityRoutes from "./communityRoutes";
import ordersRoutes from "./ordersRoutes";
import roadmapRoutes from "./roadmapRoutes";
import projectRoutes from "./projectRoutes";
import mentoringRoutes from "./mentoringRoutes";
import useAuthStore from "@/stores/useAuthStore";
import logger from "@/utils/loggerUtil.js";

// 페이지 이동 시 상단 프로그레스 바
import NProgress from "nprogress";
import "nprogress/nprogress.css";

/**
 * API
 */
import userAPI from "@/api/user";

// NProgress 설정: 스피너 숨기고 막대바만, 약간의 최소 진행률
NProgress.configure({ showSpinner: false, trickleSpeed: 120, minimum: 0.1 });

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_BASE_URL),
  routes: [
    {
      path: "/:pathMatch(.*)",
      name: "notFound",
      redirect: "/error",
    },
    {
      path: "/",
      name: "index",
      redirect: { name: "main" },
    },
    {
      name: "error",
      path: "/error",
      component: () => import("@/views/base/BaseError.vue"),
      meta: {
        title: "에러:안내 페이지",
        requiresAuth: false,
      },
    },
    mainRoutes,
    userRoutes,
    courseRoutes,
    communityRoutes,
    ordersRoutes,
    lectureRoutes,
    roadmapRoutes,
    projectRoutes,
    mentoringRoutes
  ],

  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return { top: 0 }; // 라우팅 시 페이지 맨 위로 스크롤 이동
    }
  },
});


/**
 * 전역 beforEach 가드
 *
 * 네비게이션이 트리거될 때마다 가드가 작성 순서에 따라 호출되기 전의
 * 모든 경우에 발생합니다. 가드는 비동기식으로 실행 될 수 있으며
 * 네비게이션은 모든 훅이 해결되기 전까지 보류 중으로 간주됩니다.
 */
router.beforeEach(async (to, from, next) => {
  //페이지 이동 시작 → 상단 프로그레스 바 시작
  NProgress.start();

  document.title = to.meta.title || '따라하면서 배우는 IT';

  // 로그아웃 페이지는 토큰 검사/리다이렉트 없이 항상 진입시킨다.
  // (액세스 토큰이 만료된 상태에서도 로그아웃이 로그인 페이지로 튕기지 않고 정상 완료되도록)
  if (to.name === 'logout') {
    next();
    return;
  }

  // 주의: 과거 "동일 패스면 쿼리 초기화" 로직은 같은 경로의 쿼리 기반 네비게이션
  // (페이지네이션 ?page=&size=, 검색 ?keyword= 등)을 깨뜨려 제거함.
  // 카테고리/메뉴 링크는 이미 쿼리 없는 URL을 타겟하므로 별도 초기화가 불필요하다.

  //저장소 정보 객체
  const authStore = useAuthStore();


  if (authStore.checkLogin()) {

    try {
      const data = await userAPI.userTokenCheck();
      if (data && data.success) {
        //===> 유효한 토큰일 경우 (인터셉터가 리프레시 처리 후 성공한 경우 포함)
        next();
      } else {
        //===> 토큰 체크 실패 (인터셉터가 리프레시 시도 후에도 실패)
        //인터셉터에서 이미 로그아웃 처리했을 수 있으므로 상태 재확인
        if (authStore.checkLogin()) {
          //아직 로그인 상태면 로그아웃 처리
          authStore.logout();
        }
        next({ name: "login", query: { redirect: to.fullPath } });
      }
    } catch (error) {
      //===> 요청 자체가 실패 (인터셉터에서 리프레시 실패 → reject)
      //인터셉터에서 이미 로그아웃 + 리다이렉트 처리됨
      if (authStore.checkLogin()) {
        //네트워크 에러 등 비인증 에러인 경우 → 그냥 진행
        next();
      } else {
        next({ name: "login", query: { redirect: to.fullPath } });
      }
    }

  } else if (to.meta.requiresAuth) {
    //===> 비로그인 상태에서 권한이 필요한 메뉴 접근시 안내 페이지 처리
    //로그인 페이지 이동
    next({ name: "login", query: { redirect: to.fullPath } });
  } else {
    //===> 비로그인 상태에서 권한이 필요 없는 메뉴 접근
    //패스
    next();
  }
});


/**
 * 전역 afterEach 가드 — 페이지 이동 완료 시 프로그레스 바 종료
 */
router.afterEach(() => {
  NProgress.done();
});


/**
 * 라우터 에러 전역 핸들러
 */
router.onError((e) => {
  //이동 실패 시에도 프로그레스 바를 멈춘다(걸려있지 않도록)
  NProgress.done();
  logger.error(e.toString());
});

export default router;
