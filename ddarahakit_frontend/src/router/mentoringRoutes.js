/**
 * 멘토링 라우트 (현재 준비 중 플레이스홀더)
 */
const mentoringRoutes = {
    path: '/mentoring',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
        {
            name: 'mentoring',
            path: '',
            component: () => import('@/views/mentoring/Mentoring.vue'),
            meta: {
                title: '멘토링 | 따라학잇',
                requiresAuth: false
            }
        }
    ]
}

export default mentoringRoutes
