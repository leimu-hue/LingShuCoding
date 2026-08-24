import {
    addEdge,
    type Connection,
    type Edge,
    type Node,
    useEdgesState,
    useNodesState,
} from '@xyflow/react'
import type { AppElementNode } from '@ai-code-agent/canvas'
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

/**
 * 可视化编辑器画布逻辑 hook。
 * 封装：节点/边状态、选中态、连线与标签更新。
 */
export function useEditor() {
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
                    node.id === selected.id
                        ? { ...node, data: { ...node.data, label: value } }
                        : node,
                ),
            )
            setSelected((current) =>
                current ? { ...current, data: { ...current.data, label: value } } : null,
            )
        },
        [selected, setNodes],
    )

    return {
        nodes,
        edges,
        selected,
        onNodesChange,
        onEdgesChange,
        onConnect,
        onSelectionChange,
        updateLabel,
    }
}
