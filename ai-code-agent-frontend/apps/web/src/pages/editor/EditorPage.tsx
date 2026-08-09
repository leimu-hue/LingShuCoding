import { AppCanvas, type AppElementNode } from '@ai-code-agent/canvas'
import { addEdge, useEdgesState, useNodesState, type Connection, type Edge, type Node } from '@xyflow/react'
import { Card, Empty, Input, Typography } from 'antd'
import { useCallback, useState } from 'react'

const initialNodes: AppElementNode[] = [
  { id: 'n1', type: 'element', position: { x: 0, y: 0 }, data: { label: '需求输入' } },
  { id: 'n2', type: 'element', position: { x: 220, y: 140 }, data: { label: 'AI 生成页面' } },
  { id: 'n3', type: 'element', position: { x: 440, y: 280 }, data: { label: '数据绑定' } },
]

const initialEdges: Edge[] = [
  { id: 'e1', source: 'n1', target: 'n2' },
  { id: 'e2', source: 'n2', target: 'n3' },
]

export default function EditorPage() {
  const [nodes, setNodes, onNodesChange] = useNodesState<AppElementNode>(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)
  const [selected, setSelected] = useState<AppElementNode | null>(null)

  const onConnect = useCallback(
    (connection: Connection) => setEdges((eds) => addEdge(connection, eds)),
    [setEdges],
  )

  const onSelectionChange = useCallback((selectedNodes: Node[]) => {
    setSelected((selectedNodes[0] ?? null) as AppElementNode | null)
  }, [])

  const updateLabel = useCallback(
    (value: string) => {
      if (!selected) {
        return
      }
      setNodes((nds) =>
        nds.map((node) =>
          node.id === selected.id ? { ...node, data: { ...node.data, label: value } } : node,
        ),
      )
      setSelected((current) => (current ? { ...current, data: { ...current.data, label: value } } : null))
    },
    [selected, setNodes],
  )

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Typography.Title level={4} style={{ marginTop: 0 }}>
        可视化编辑器
      </Typography.Title>
      <div style={{ flex: 1, display: 'flex', gap: 12, minHeight: 0 }}>
        <div style={{ flex: 1, border: '1px solid #f0f0f0', borderRadius: 8, overflow: 'hidden' }}>
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
              <Typography.Text type="secondary">节点 ID：{selected.id}</Typography.Text>
              <Input
                addonBefore="名称"
                value={selected.data?.label ?? ''}
                onChange={(event) => updateLabel(event.target.value)}
              />
            </div>
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="选中画布中的元素以编辑属性" />
          )}
        </Card>
      </div>
    </div>
  )
}