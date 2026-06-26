import { ref, onUnmounted } from 'vue'

/**
 * 멘토링 웹소켓 공통 훅
 * - 자동 재연결(지수 백오프)
 * - 연결 전 메시지 큐잉
 * - 메시지 리스너 등록/해제
 */
export const useMentoringSocket = (options = {}) => {
    const socket = ref(null)
    const isOpen = ref(false)

    const listeners = new Set()
    const messageQueue = []
    let reconnectTimer = null
    let reconnectAttempt = 0
    let closedByUser = false

    const baseDelay = options.baseDelay ?? 800
    const maxDelay = options.maxDelay ?? 10000

    const resolveUrl = () => {
        if (options.url) return options.url
        const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://'
        return `${protocol}${window.location.host}/ws`
    }

    const flushQueue = () => {
        if (!socket.value || socket.value.readyState !== WebSocket.OPEN) return
        while (messageQueue.length > 0) {
            socket.value.send(messageQueue.shift())
        }
    }

    const scheduleReconnect = () => {
        if (closedByUser) return
        const delay = Math.min(baseDelay * 2 ** reconnectAttempt, maxDelay)
        reconnectAttempt += 1
        reconnectTimer = setTimeout(() => {
            connect()
        }, delay)
    }

    const connect = () => {
        clearTimeout(reconnectTimer)

        const ws = new WebSocket(resolveUrl())
        socket.value = ws

        ws.addEventListener('open', () => {
            isOpen.value = true
            reconnectAttempt = 0
            flushQueue()
        })

        ws.addEventListener('message', (event) => {
            let payload = null
            try {
                payload = JSON.parse(event.data)
            } catch (_) {
                payload = event.data
            }

            listeners.forEach((handler) => {
                try {
                    handler(payload)
                } catch (error) {
                    console.error('[mentoring-socket] message handler error', error)
                }
            })
        })

        ws.addEventListener('close', () => {
            isOpen.value = false
            scheduleReconnect()
        })

        ws.addEventListener('error', () => {
            isOpen.value = false
        })
    }

    const send = (message) => {
        const serialized = typeof message === 'string' ? message : JSON.stringify(message)
        if (socket.value && socket.value.readyState === WebSocket.OPEN) {
            socket.value.send(serialized)
        } else {
            messageQueue.push(serialized)
        }
    }

    const onMessage = (handler) => {
        listeners.add(handler)
        return () => listeners.delete(handler)
    }

    const close = () => {
        closedByUser = true
        clearTimeout(reconnectTimer)
        if (socket.value) {
            socket.value.close()
        }
    }

    connect()

    onUnmounted(() => {
        close()
    })

    return {
        socket,
        isOpen,
        connect,
        send,
        onMessage,
        close
    }
}
