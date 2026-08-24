import { AppCanvas } from '@ai-code-agent/canvas'
import { Card, Empty, Flex, Input, Typography } from 'antd'
import { useEditor } from './hooks/useEditor'

export default function EditorPage() {
    const {
        nodes,
        edges,
        selected,
        onNodesChange,
        onEdgesChange,
        onConnect,
        onSelectionChange,
        updateLabel,
    } = useEditor()

    return (
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                可视化编辑器
            </Typography.Title>
            <div style={{ flex: 1, display: 'flex', gap: 12, minHeight: 0 }}>
                <div
                    style={{
                        flex: 1,
                        border: '1px solid #f0f0f0',
                        borderRadius: 8,
                        overflow: 'hidden',
                    }}
                >
                    <AppCanvas
                        nodes={nodes}
                        edges={edges}
                        onNodesChange={onNodesChange}
                        onEdgesChange={onEdgesChange}
                        onConnect={onConnect}
                        onSelectionChange={onSelectionChange}
                    />
                </div>
                <Card title="元素属性" size="small" style={{ width: 260, minWidth: 200 }}>
                    {selected ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                            <Typography.Text type="secondary">
                                节点 ID：{selected.id}
                            </Typography.Text>
                            <Flex align="center" gap={8}>
                                <Typography.Text style={{ flexShrink: 0 }}>名称</Typography.Text>
                                <Input
                                    value={selected.data?.label ?? ''}
                                    onChange={(event) => updateLabel(event.target.value)}
                                />
                            </Flex>
                        </div>
                    ) : (
                        <Empty
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            description="选中画布中的元素以编辑属性"
                        />
                    )}
                </Card>
            </div>
        </div>
    )
}
