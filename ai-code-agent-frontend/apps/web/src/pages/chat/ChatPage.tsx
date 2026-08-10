import { CommentOutlined } from '@ant-design/icons'
import { Alert, Card, Empty, Typography } from 'antd'

export default function ChatPage() {
    return (
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Typography.Title level={4}>AI 对话生成</Typography.Title>
            <Alert
                type="info"
                showIcon
                title="SSE 流式能力已就绪"
                description="通过 @ai-code-agent/shared 的 streamSse 可与后端 AI 接口流式对话，本页交互待实现。"
                style={{ marginBottom: 16 }}
            />
            <Card style={{ flex: 1 }}>
                <Empty
                    image={<CommentOutlined style={{ fontSize: 64, color: '#bfbfbf' }} />}
                    description="对话式需求描述 → AI 生成应用（待实现）"
                />
            </Card>
        </div>
    )
}
