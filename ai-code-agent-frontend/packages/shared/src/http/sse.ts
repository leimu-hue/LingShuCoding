import { authHeaders } from '../auth.ts'

export interface SseEvent {
    data: string
    event?: string
    id?: string
}

export interface StreamSseOptions {
    url: string
    method?: 'GET' | 'POST'
    body?: unknown
    headers?: Record<string, string>
    signal?: AbortSignal
    onEvent?: (event: SseEvent) => void
    onDone?: () => void
    onError?: (error: unknown) => void
}

/**
 * 解析单个 SSE 事件块（以空行分隔的一段字段）。
 * 规范要点：
 * - 以 `:` 开头的行是注释，忽略
 * - 字段名与值之间可有可选的一个前导空格
 * - 多个 `data:` 行用换行符拼接
 * - 支持 CRLF 换行
 */
export function parseSseEvent(block: string): SseEvent | null {
    let data = ''
    let event: string | undefined
    let id: string | undefined
    for (const rawLine of block.split('\n')) {
        const line = rawLine.replace(/\r$/, '')
        if (line === '' || line.startsWith(':')) {
            continue
        }
        const idx = line.indexOf(':')
        if (idx === -1) {
            continue
        }
        const field = line.slice(0, idx)
        let value = line.slice(idx + 1)
        if (value.startsWith(' ')) {
            value = value.slice(1)
        }
        if (field === 'data') {
            data += `${value}\n`
        } else if (field === 'event') {
            event = value
        } else if (field === 'id') {
            id = value
        }
    }
    if (!data) {
        return null
    }
    const result: SseEvent = { data: data.replace(/\n$/, '') }
    if (event !== undefined) {
        result.event = event
    }
    if (id !== undefined) {
        result.id = id
    }
    return result
}

export async function streamSse(options: StreamSseOptions): Promise<void> {
    const { url, method = 'GET', body, headers, signal, onEvent, onDone, onError } = options
    try {
        const response = await fetch(url, {
            method,
            headers: {
                Accept: 'text/event-stream',
                ...(body ? { 'Content-Type': 'application/json' } : {}),
                ...authHeaders(),
                ...headers,
            },
            body: body ? JSON.stringify(body) : undefined,
            signal,
        })
        if (!response.ok || !response.body) {
            throw new Error(`SSE 请求失败: HTTP ${response.status}`)
        }
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        for (;;) {
            const { done, value } = await reader.read()
            if (done) {
                break
            }
            // 统一换行符，按事件边界（空行）切分
            buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
            let boundary: number
            while ((boundary = buffer.indexOf('\n\n')) !== -1) {
                const rawEvent = buffer.slice(0, boundary)
                buffer = buffer.slice(boundary + 2)
                const event = parseSseEvent(rawEvent)
                if (event) {
                    onEvent?.(event)
                }
            }
        }
        onDone?.()
    } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
            return
        }
        onError?.(error)
    }
}
