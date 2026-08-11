import assert from 'node:assert/strict'
import { test } from 'node:test'
import { parseSseEvent } from '../src/http/sse.ts'

test('解析单行 data', () => {
    const event = parseSseEvent('data: hello\n')
    assert.deepEqual(event, { data: 'hello' })
})

test('多行 data 用换行拼接', () => {
    const event = parseSseEvent('data: line1\ndata: line2\n')
    assert.deepEqual(event, { data: 'line1\nline2' })
})

test('忽略注释行与空字段', () => {
    const event = parseSseEvent(': this is a comment\ndata: ok\n\n')
    assert.deepEqual(event, { data: 'ok' })
})

test('解析 event / id 字段并去除值前导空格', () => {
    const event = parseSseEvent('event: message\nid: 42\ndata: hi\n')
    assert.deepEqual(event, { data: 'hi', event: 'message', id: '42' })
})

test('支持 CRLF 换行', () => {
    const event = parseSseEvent('data: crlf\r\nid: 7\r\n')
    assert.deepEqual(event, { data: 'crlf', id: '7' })
})

test('无 data 时返回 null', () => {
    assert.equal(parseSseEvent(': only comment\n'), null)
    assert.equal(parseSseEvent('event: ping\n'), null)
})
