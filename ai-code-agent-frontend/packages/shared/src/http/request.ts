import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig } from 'axios'
import { authHeaders } from '@ai-code-agent/shared/auth'
import { reportApiError, reportSessionExpired } from '@ai-code-agent/shared/errorBus'
import { type Result, SUCCESS_CODE } from '@ai-code-agent/shared/types/result'

export interface RequestOptions extends AxiosRequestConfig {
    /**
     * 不检查统一响应体 code，直接返回原始响应（用于非 Result 包装的接口）
     */
    raw?: boolean
    /**
     * 为 true 时不向全局错误总线上报（由调用方自行处理错误 UI，例如页面内联提示）
     */
    silent?: boolean
}

export const request = axios.create({
    baseURL: import.meta.env?.VITE_API_BASE_URL ?? '/api',
    timeout: 30_000,
})

// 请求拦截器：注入鉴权头
request.interceptors.request.use((config) => {
    config.headers = new AxiosHeaders(config.headers).set(authHeaders())
    return config
})

request.interceptors.response.use(
    (response) => {
        if (response.config.method === 'options') {
            return response
        }
        const body = response.data as Result
        if (body && typeof body === 'object' && 'code' in body && body.code !== SUCCESS_CODE) {
            const message = body.message || `业务错误码: ${body.code}`
            if (!(response.config as RequestOptions).silent) {
                reportApiError(message)
            }
            return Promise.reject(new Error(message))
        }
        return response
    },
    (error: AxiosError) => {
        // 会话过期 / 未登录：统一走会话过期事件（清凭证 + 跳转登录 + 去重提示），
        // 不再按普通错误弹「未授权」提示，避免并发请求各自弹一次。
        if (error.response?.status === 401) {
            reportSessionExpired()
            return Promise.reject(new Error('登录已过期，请重新登录'))
        }
        const message = error.response?.data
            ? String((error.response.data as Result).message ?? `HTTP ${error.response.status}`)
            : error.message
        if (!(error.config as RequestOptions | undefined)?.silent) {
            reportApiError(message)
        }
        return Promise.reject(new Error(message))
    },
)

export async function http<T>(config: RequestOptions): Promise<T> {
    const response = await request.request<Result<T>>(config)
    return response.data.data
}

export default http
