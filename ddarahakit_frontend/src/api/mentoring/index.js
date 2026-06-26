import $axios from '@/plugins/axiosInterceptor'

/**
 * 멘토링 세션 목록 조회
 */
const list = async (req = {}) => {
    let data = {}

    const params = {
        page: req.page,
        size: req.size,
        keyword: req.keyword,
        status: req.status
    }

    await $axios
        .get('/mentoring', { params })
        .then((res) => {
            data = res.data
        })
        .catch((error) => {
            data = error.data
        })

    return data
}

/**
 * 멘토링 세션 상세/대화 내역 조회
 */
const detail = async (sessionId) => {
    let data = {}

    await $axios
        .get(`/mentoring/${sessionId}`)
        .then((res) => {
            data = res.data
        })
        .catch((error) => {
            data = error.data
        })

    return data
}

/**
 * 멘토링 세션 메시지 목록 조회
 */
const messages = async (sessionId, req = {}) => {
    let data = {}

    const params = {
        page: req.page,
        size: req.size,
        before: req.before
    }

    await $axios
        .get(`/mentoring/${sessionId}/messages`, { params })
        .then((res) => {
            data = res.data
        })
        .catch((error) => {
            data = error.data
        })

    return data
}

/**
 * 멘토링 세션 메시지 전송
 */
const sendMessage = async (sessionId, req) => {
    let data = {}

    const messageInfo = {
        message: req.message,
        messageType: req.messageType || 'TEXT',
        clientMessageId: req.clientMessageId
    }

    await $axios
        .post(`/mentoring/${sessionId}/messages`, messageInfo)
        .then((res) => {
            data = res.data
        })
        .catch((error) => {
            data = error.data
        })

    return data
}

/**
 * 멘토링 세션 생성
 */
const createSession = async (req) => {
    let data = {}

    const sessionInfo = {
        mentorIdx: req.mentorIdx,
        subject: req.subject,
        scheduledAt: req.scheduledAt,
        message: req.message
    }

    await $axios
        .post('/mentoring', sessionInfo)
        .then((res) => {
            data = res.data
        })
        .catch((error) => {
            data = error.data
        })

    return data
}

/**
 * 멘토링 세션 읽음 처리
 */
const markAsRead = async (sessionId) => {
    let data = {}

    await $axios
        .patch(`/mentoring/${sessionId}/read`)
        .then((res) => {
            data = res.data
        })
        .catch((error) => {
            data = error.data
        })

    return data
}

/**
 * 멘토링 세션 종료/삭제
 */
const closeSession = async (sessionId) => {
    let data = {}

    await $axios
        .delete(`/mentoring/${sessionId}`)
        .then((res) => {
            data = res.data
        })
        .catch((error) => {
            data = error.data
        })

    return data
}

export default {
    list,
    detail,
    messages,
    sendMessage,
    createSession,
    markAsRead,
    closeSession
}
