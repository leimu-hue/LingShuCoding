import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
    onApiError,
    onSessionExpired,
    reportApiError,
    reportSessionExpired,
} from '../src/errorBus.ts'

test('reportSessionExpired 触发所有订阅者', () => {
    const calls: string[] = []
    const off1 = onSessionExpired(() => calls.push('a'))
    const off2 = onSessionExpired(() => calls.push('b'))

    reportSessionExpired()

    assert.deepEqual(calls.sort(), ['a', 'b'])

    off1()
    off2()
})

test('onSessionExpired 返回的取消订阅函数可移除订阅', () => {
    let count = 0
    const off = onSessionExpired(() => {
        count += 1
    })

    reportSessionExpired()
    assert.equal(count, 1)

    off()
    reportSessionExpired()
    assert.equal(count, 1)
})

test('API 错误与会话过期事件互不干扰', () => {
    const apiErrors: string[] = []
    let expired = 0
    const offApi = onApiError((msg) => apiErrors.push(msg))
    const offExpired = onSessionExpired(() => {
        expired += 1
    })

    reportApiError('业务错误')
    reportSessionExpired()

    assert.deepEqual(apiErrors, ['业务错误'])
    assert.equal(expired, 1)

    offApi()
    offExpired()
})
