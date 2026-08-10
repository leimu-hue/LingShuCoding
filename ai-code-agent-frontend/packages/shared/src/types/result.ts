export const SUCCESS_CODE = 0

export interface Result<T = unknown> {
    code: number
    message: string
    data: T
}
