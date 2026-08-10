import { Handle, type Node, type NodeProps, Position } from '@xyflow/react'
import type { ReactNode } from 'react'

export interface ElementNodeData extends Record<string, unknown> {
    label: string
    icon?: ReactNode
}

export type AppElementNode = Node<ElementNodeData, 'element'>

const containerStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    padding: '8px 14px',
    borderRadius: 8,
    border: '1px solid #d9d9d9',
    background: '#fff',
    boxShadow: '0 1px 4px rgba(0, 0, 0, 0.08)',
    fontSize: 14,
}

const selectedStyle: React.CSSProperties = {
    borderColor: '#1677ff',
    boxShadow: '0 0 0 2px rgba(22, 119, 255, 0.2)',
}

export function ElementNode({ data, selected }: NodeProps<AppElementNode>) {
    return (
        <div style={{ ...containerStyle, ...(selected ? selectedStyle : {}) }}>
            <Handle type="target" position={Position.Top} style={{ opacity: 0 }} />
            {data?.icon}
            <span>{data?.label}</span>
            <Handle type="source" position={Position.Bottom} style={{ opacity: 0 }} />
        </div>
    )
}
