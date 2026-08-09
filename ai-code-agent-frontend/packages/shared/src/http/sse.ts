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

function parseChunk(chunk: string): SseEvent | null {
  let data = ''
  let event: string | undefined
  let id: string | undefined
  for (const line of chunk.split('\n')) {
    if (line.startsWith('data:')) {
      data += `${line.slice(5).trimStart()}\n`
    } else if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('id:')) {
      id = line.slice(3).trim()
    }
  }
  if (!data) {
    return null
  }
  return { data: data.replace(/\n$/, ''), event, id }
}

export async function streamSse(options: StreamSseOptions): Promise<void> {
  const { url, method = 'GET', body, headers, signal, onEvent, onDone, onError } = options
  try {
    const response = await fetch(url, {
      method,
      headers: { Accept: 'text/event-stream', ...(body ? { 'Content-Type': 'application/json' } : {}), ...headers },
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
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop() ?? ''
      for (const part of parts) {
        const event = parseChunk(part)
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