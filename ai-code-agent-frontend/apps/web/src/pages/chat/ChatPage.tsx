import { SendOutlined, StopOutlined } from '@ant-design/icons'
import { App as AntdApp, Button, Input, Spin, Typography, theme } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { streamSse, isUnauthorizedError } from '@ai-code-agent/shared'

interface ChatMessage {
    id: string
    role: 'user' | 'assistant'
    content: string
}

export default function ChatPage() {
    const { message } = AntdApp.useApp()
    const { token } = theme.useToken()
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [input, setInput] = useState('')
    const [loading, setLoading] = useState(false)
    const abortRef = useRef<AbortController | null>(null)
    const listRef = useRef<HTMLDivElement>(null)

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

    return (
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                AI 对话生成
            </Typography.Title>
            <div
                ref={listRef}
                style={{
                    flex: 1,
                    overflow: 'auto',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 12,
                    padding: '4px 4px 12px',
                }}
            >
                {messages.length === 0 && (
                    <Typography.Text type="secondary">
                        描述你的需求，AI 将流式生成应用（需后端 /api/chat 提供 SSE 流）。
                    </Typography.Text>
                )}
                {messages.map((m) => (
                    <div
                        key={m.id}
                        style={{
                            alignSelf: m.role === 'user' ? 'flex-end' : 'flex-start',
                            maxWidth: '80%',
                            padding: '8px 12px',
                            borderRadius: 8,
                            background:
                                m.role === 'user' ? token.colorPrimary : token.colorBgContainer,
                            color: m.role === 'user' ? '#fff' : token.colorText,
                            border: `1px solid ${token.colorBorderSecondary}`,
                            whiteSpace: 'pre-wrap',
                            wordBreak: 'break-word',
                        }}
                    >
                        {m.content || (loading ? <Spin size="small" /> : null)}
                    </div>
                ))}
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
                <Input.TextArea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    placeholder="输入需求描述，回车发送"
                    autoSize={{ minRows: 1, maxRows: 4 }}
                    onPressEnter={(e) => {
                        if (!e.shiftKey) {
                            e.preventDefault()
                            send()
                        }
                    }}
                />
                {loading ? (
                    <Button icon={<StopOutlined />} onClick={stop} danger>
                        停止
                    </Button>
                ) : (
                    <Button
                        type="primary"
                        icon={<SendOutlined />}
                        onClick={send}
                        disabled={!input.trim()}
                    >
                        发送
                    </Button>
                )}
            </div>
        </div>
    )
}
