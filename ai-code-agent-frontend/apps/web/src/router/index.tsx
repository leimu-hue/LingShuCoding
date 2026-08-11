import { lazy, Suspense, type ReactNode } from 'react'
import { Spin } from 'antd'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import AdminLayout from '../layouts/AdminLayout'
import FrontLayout from '../layouts/FrontLayout'

// 路由级懒加载：仅在使用时按需加载对应页面，
// 重型依赖（如编辑器页的 @xyflow/react）因此不会进入首屏包。
const ChatPage = lazy(() => import('../pages/chat/ChatPage'))
const ConsolePage = lazy(() => import('../pages/console/ConsolePage'))
const EditorPage = lazy(() => import('../pages/editor/EditorPage'))

function lazyPage(node: ReactNode) {
    return (
        <Suspense
            fallback={
                <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
                    <Spin />
                </div>
            }
        >
            {node}
        </Suspense>
    )
}

// 后台管理界面路由：console/editor 为管理员功能，仅挂载在 /admin 下
const adminRoutes = [
    { path: 'chat', element: lazyPage(<ChatPage />) },
    { path: 'console', element: lazyPage(<ConsolePage />) },
    { path: 'editor', element: lazyPage(<EditorPage />) },
]

export const router = createBrowserRouter([
    {
        // 前台界面：顶部导航 + 路由内容区 + 底部页脚（仅普通用户功能）
        path: '/',
        element: <FrontLayout />,
        children: [
            { index: true, element: <Navigate to="/chat" replace /> },
            { path: 'chat', element: lazyPage(<ChatPage />) },
        ],
    },
    {
        // 后台管理界面：侧边栏布局
        path: '/admin',
        element: <AdminLayout />,
        children: [{ index: true, element: <Navigate to="/admin/chat" replace /> }, ...adminRoutes],
    },
])
