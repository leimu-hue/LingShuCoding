import { SendOutlined, StopOutlined } from '@ant-design/icons'
import { Button, Input, Spin, Typography, theme } from 'antd'
import { useChat } from './hooks/useChat'

export default function ChatPage() {
    const { token } = theme.useToken()
    const { messages, input, setInput, loading, listRef, send, stop } = useChat()

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
