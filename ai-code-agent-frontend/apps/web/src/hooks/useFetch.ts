import { useCallback, useEffect, useRef, useState } from 'react'
import { http, type RequestOptions } from '@ai-code-agent/shared'

export interface UseFetchResult<T> {
    data: T | null
    loading: boolean
    error: string | null
    /** 手动（重新）发起请求；可通过 override 覆盖本次请求配置 */
    run: (override?: RequestOptions) => Promise<T | null>
}

/**
 * 数据请求 hook：统一管理 loading / error / data，并在组件卸载时自动忽略迟到的结果。
 * 默认挂载即请求；传 { immediate: false } 可改为手动触发。
 * 通过 silent 避免与全局错误提示重复，错误由调用方以内联 UI 呈现。
 */
export function useFetch<T>(
    config: RequestOptions,
    options?: { immediate?: boolean },
): UseFetchResult<T> {
    const [data, setData] = useState<T | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const configRef = useRef(config)
    // 用 ref 保存最新 config，避免在 config 变化时重复触发请求（最新值模式）
    // eslint-disable-next-line react-hooks/refs -- 在 render 期间写入 ref 以持有最新 config
    configRef.current = config
    const mountedRef = useRef(true)
    useEffect(() => {
        mountedRef.current = true
        return () => {
            mountedRef.current = false
        }
    }, [])

    const run = useCallback(async (override?: RequestOptions): Promise<T | null> => {
        const merged = { ...configRef.current, ...override, silent: true } as RequestOptions
        setLoading(true)
        setError(null)
        try {
            const result = await http<T>(merged)
            if (mountedRef.current) {
                setData(result ?? null)
            }
            return result
        } catch (reason) {
            const message = reason instanceof Error ? reason.message : String(reason)
            if (mountedRef.current) {
                setError(message)
            }
            return null
        } finally {
            if (mountedRef.current) {
                setLoading(false)
            }
        }
    }, [])

    useEffect(() => {
        if (options?.immediate !== false) {
            // 挂载即拉取是预期行为
            // eslint-disable-next-line react-hooks/set-state-in-effect -- 挂载时发起请求是设计意图
            void run()
        }
    }, [run, options?.immediate])

    return { data, loading, error, run }
}
