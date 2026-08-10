import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { type Result, SUCCESS_CODE } from '../types/result'

export interface RequestOptions extends AxiosRequestConfig {
    /**
     * 不检查统一响应体 code，直接返回原始响应（用于非 Result 包装的接口）
     */
    raw?: boolean
}

export const request = axios.create({
    baseURL: import.meta.env?.VITE_API_BASE_URL ?? '/api',
    timeout: 30_000,
})

request.interceptors.response.use(
    (response) => {
        if (response.config.method === 'options') {
            return response
        }
        const body = response.data as Result
        if (body && typeof body === 'object' && 'code' in body && body.code !== SUCCESS_CODE) {
            return Promise.reject(new Error(body.message || `业务错误码: ${body.code}`))
        }
        return response
    },
    (error: AxiosError) => {
        const message = error.response?.data
            ? String((error.response.data as Result).message ?? `HTTP ${error.response.status}`)
            : error.message
        return Promise.reject(new Error(message))
    },
)

export async function http<T>(config: RequestOptions): Promise<T> {
    const response = await request.request<Result<T>>(config)
    return response.data.data
}

export default http
