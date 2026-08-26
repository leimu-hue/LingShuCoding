/** 展示创建时间（ISO 串 → YYYY-MM-DD HH:mm） */
export function formatDate(value: string | null): string {
    if (typeof value !== 'string' || value.length === 0) {
        return '-'
    }
    return value.replace('T', ' ').slice(0, 16)
}
