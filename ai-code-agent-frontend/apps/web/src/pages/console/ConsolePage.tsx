import { Alert, Card, Typography } from 'antd'

/**
 * 应用管理页（占位）
 * 原 demo 用户列表接口已随后端用户模块重构移除；
 * 待用户模块后端落地后，前端登录/当前用户/管理端页面另行规划。
 */
export default function ConsolePage() {
    return (
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 12 }}>
            <Typography.Title level={4} style={{ margin: 0 }}>
                应用管理
            </Typography.Title>
            <Card style={{ flex: 1, overflow: 'auto' }}>
                <Alert
                    type="info"
                    showIcon
                    title="页面建设中"
                    description="后端用户模块（RBAC + Token 认证）已排期，登录与管理端页面将在此接入。"
                />
            </Card>
        </div>
    )
}
