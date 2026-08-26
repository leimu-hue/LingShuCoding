export { SUCCESS_CODE } from '@ai-code-agent/shared/types/result'
export type { Result } from '@ai-code-agent/shared/types/result'
export { http, request } from '@ai-code-agent/shared/http/request'
export type { RequestOptions } from '@ai-code-agent/shared/http/request'
export { streamSse, parseSseEvent, isUnauthorizedError } from '@ai-code-agent/shared/http/sse'
export type { SseEvent, StreamSseOptions, SseRequestError } from '@ai-code-agent/shared/http/sse'
export { getAuthToken, setAuthToken, authHeaders } from '@ai-code-agent/shared/auth'
export {
    onApiError,
    reportApiError,
    onSessionExpired,
    reportSessionExpired,
} from '@ai-code-agent/shared/errorBus'
export type { ApiErrorHandler, SessionExpiredHandler } from '@ai-code-agent/shared/errorBus'
