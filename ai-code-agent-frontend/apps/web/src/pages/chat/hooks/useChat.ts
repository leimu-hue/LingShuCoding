import { App as AntdApp } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { isUnauthorizedError, streamSse } from '@ai-code-agent/shared'

export interface ChatMessage {
    id: string
    role: 'user' | 'assistant'
    content: string
}

/**
 * AI 对话流式交互 hook。
 * 封装：消息状态、输入状态、SSE 流式请求、停止控制与自动滚动锚点。
 */
export function useChat() {
    const { message } = AntdApp.useApp()
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [input, setInput] = useState('')
    const [loading, setLoading] = useState(false)
    const abortRef = useRef<AbortController | null>(null)
    const listRef = useRef<HTMLDivElement>(null)

    // 消息变化时自动滚动到底部
    useEffect(() => {
        listRef.current?.scrollTo({ top: listRef.current.scrollHeight })
    }, [messages])

    const send = useCallback(() => {
        const text = input.trim()
        if (!text || loading) {
            return
        }
        const botId = `a-${Date.now()}`
        setMessages((prev) => [
            ...prev,
            { id: `u-${Date.now()}`, role: 'user', content: text },
            { id: botId, role: 'assistant', content: '' },
        ])
        setInput('')
        setLoading(true)
        const controller = new AbortController()
        abortRef.current = controller
        streamSse({
            url: '/api/chat',
            method: 'POST',
            body: { message: text },
            signal: controller.signal,
            onEvent: (event) => {
                setMessages((prev) =>
                    prev.map((m) =>
                        m.id === botId ? { ...m, content: m.content + event.data } : m,
                    ),
                )
            },
            onDone: () => setLoading(false),
            onError: (err) => {
                setLoading(false)
                if (isUnauthorizedError(err)) {
                    // 401 会话过期已由全局桥处理（清凭证 + 跳转登录 + 提示一次），此处不再重复弹错误
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === botId && !m.content
                                ? { ...m, content: '（登录已过期，请重新登录）' }
                                : m,
                        ),
                    )
                    return
                }
                message.error(err instanceof Error ? err.message : '对话请求失败')
                setMessages((prev) =>
                    prev.map((m) =>
                        m.id === botId && !m.content
                            ? {
                                  ...m,
                                  content: '（请求失败，请确认后端 /api/chat 已启动并支持 SSE）',
                              }
                            : m,
                    ),
                )
            },
        })
    }, [input, loading, message])

    const stop = useCallback(() => {
        abortRef.current?.abort()
        setLoading(false)
    }, [])

    return { messages, input, setInput, loading, listRef, send, stop }
}
