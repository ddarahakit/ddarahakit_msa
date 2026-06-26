<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({
    heading: {
        type: String,
        default: 'Dashboard'
    }
})

const route = useRoute()

const menuItems = computed(() => [
    {
        key: 'dashboard',
        label: '대시보드',
        icon: 'fa-solid fa-house-chimney text-sm',
        to: { name: 'dashboard' }
    },
    {
        key: 'mentoring',
        label: '멘토링',
        icon: 'fa-solid fa-comments text-sm',
        to: { name: 'mentoring' },
        // 멘토링은 history/schedule 등 하위 라우트가 있어 경로 prefix 로 활성 판정
        match: (r) => r.path.startsWith('/mentoring')
    },
    {
        key: 'accountSecurity',
        label: '비밀번호 변경',
        icon: 'fa-solid fa-lock text-sm',
        to: { name: 'accountSecurity' }
    },
    {
        key: 'paymentHistory',
        label: '결제 내역',
        icon: 'fa-solid fa-credit-card text-sm',
        to: { name: 'paymentHistory' }
    }
])

const isActiveMenu = (item) => {
    if (typeof item.match === 'function') return item.match(route)
    return route.name === item.key
}
</script>

<template>
    <aside class="w-full lg:w-60 shrink-0">
        <div class="sticky top-28 space-y-1">
            <p class="px-4 text-[11px] font-bold text-slate-400 uppercase tracking-widest mb-4">
                {{ props.heading }}
            </p>

            <RouterLink
                v-for="item in menuItems"
                :key="item.key"
                :to="item.to"
                class="flex items-center gap-3 px-4 py-3 transition-all font-medium"
                :class="isActiveMenu(item)
                    ? 'sidebar-item-active font-bold'
                    : 'text-slate-500 hover:text-slate-800'">
                <i :class="item.icon"></i> {{ item.label }}
            </RouterLink>
        </div>
    </aside>
</template>

<style scoped>
.sidebar-item-active {
    color: #14BCED;
    background: rgba(20, 188, 237, 0.06);
    border-radius: 0.75rem;
}
</style>
