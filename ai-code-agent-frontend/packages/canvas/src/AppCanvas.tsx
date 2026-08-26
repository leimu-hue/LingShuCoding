import {
    Background,
    BackgroundVariant,
    Controls,
    type Edge,
    MiniMap,
    type Node,
    type NodeTypes,
    type OnConnect,
    type OnEdgesChange,
    type OnNodesChange,
    type OnSelectionChangeParams,
    ReactFlow,
} from '@xyflow/react'
import { ElementNode } from '@ai-code-agent/canvas/ElementNode'

const nodeTypes: NodeTypes = { element: ElementNode }

export interface AppCanvasProps<TNode extends Node = Node> {
    nodes: TNode[]
    edges: Edge[]
    onNodesChange: OnNodesChange<TNode>
    onEdgesChange: OnEdgesChange<Edge>
    onConnect: OnConnect
    onSelectionChange?: (nodes: TNode[]) => void
    fitView?: boolean
    className?: string
}

export function AppCanvas<TNode extends Node = Node>({
    nodes,
    edges,
    onNodesChange,
    onEdgesChange,
    onConnect,
    onSelectionChange,
    fitView = true,
    className,
}: AppCanvasProps<TNode>) {
    return (
        <div className={className} style={{ width: '100%', height: '100%' }}>
            <ReactFlow<TNode>
                nodes={nodes}
                edges={edges}
                nodeTypes={nodeTypes}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                onSelectionChange={(params: OnSelectionChangeParams<TNode>) =>
                    onSelectionChange?.(params.nodes)
                }
                fitView={fitView}
                proOptions={{ hideAttribution: true }}
            >
                <Background variant={BackgroundVariant.Dots} gap={16} size={1} />
                <Controls />
                <MiniMap pannable zoomable />
            </ReactFlow>
        </div>
    )
}
