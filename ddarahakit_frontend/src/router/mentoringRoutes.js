/**
 * 멘토링 라우트
 */
const mentoringRoutes = {
    path: '/mentoring',
    component: () => import('@/layouts/MentoringLayout.vue'),
    children: [
        {
            name: 'mentoring',
            path: '',
            redirect: { name: 'mentoringHistory' },
            meta: {
                title: '멘토링 | 따라학잇',
                requiresAuth: false
            }
        },
        {
            name: 'mentoringHistory',
            path: 'history/:sessionId?',
            component: () => import('@/views/user/Mentoring.vue'),
            meta: {
                title: '멘토링 기록 | 따라학잇',
                requiresAuth: false
            }
        },
        {
            name: 'mentoringSchedule',
            path: 'schedule',
            component: () => import('@/views/user/Mentoring.vue'),
            meta: {
                title: '멘토링 일정 | 따라학잇',
                requiresAuth: false
            }
        },
        {
            name: 'mentoringScheduleDetail',
            path: 'schedule/:sessionId',
            component: () => import('@/views/user/Mentoring.vue'),
            meta: {
                title: '멘토링 일정 상세 | 따라학잇',
                requiresAuth: false
            }
        }
    ]
}

export default mentoringRoutes  
