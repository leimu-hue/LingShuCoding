import { App, Form, Input, Modal } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import * as adminApi from '@/api/admin'
import type { UserAdminDTO } from '@/types/user'

interface ResetPasswordModalProps {
    user: UserAdminDTO | null
    onClose: () => void
}

/** 重置密码弹窗（独立 UI 区块 + 其自身的提交逻辑） */
export default function ResetPasswordModal({ user, onClose }: ResetPasswordModalProps) {
    const { message } = App.useApp()
    const queryClient = useQueryClient()
    const [form] = Form.useForm<{ newPassword: string }>()

    const resetPassword = useMutation({
        mutationFn: ({ id, newPassword }: { id: number; newPassword: string }) =>
            adminApi.resetPassword(id, newPassword),
        onSuccess: () => {
            message.success('密码已重置')
            form.resetFields()
            onClose()
            void queryClient.invalidateQueries({ queryKey: ['users'] })
        },
    })

    const handleOk = async () => {
        if (!user) {
            return
        }
        let values: { newPassword: string }
        try {
            values = await form.validateFields()
        } catch {
            // 校验失败，antd 已内联提示，不发起请求
            return
        }
        resetPassword.mutate({ id: user.id, newPassword: values.newPassword })
    }

    return (
        <Modal
            open={!!user}
            title={`重置密码 - ${user?.username ?? ''}`}
            onOk={handleOk}
            onCancel={onClose}
            confirmLoading={resetPassword.isPending}
            destroyOnHidden
        >
            <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
                <Form.Item
                    name="newPassword"
                    label="新密码"
                    rules={[
                        { required: true, message: '请输入新密码' },
                        { min: 6, max: 64, message: '密码长度 6-64 位' },
                    ]}
                >
                    <Input.Password placeholder="新密码（6-64 位）" />
                </Form.Item>
            </Form>
        </Modal>
    )
}
