import { createBrowserRouter, Navigate } from 'react-router-dom'
import ChatPage from '../pages/chat/ChatPage'
import ConsolePage from '../pages/console/ConsolePage'
import EditorPage from '../pages/editor/EditorPage'
import AdminLayout from '../layouts/AdminLayout'
import FrontLayout from '../layouts/FrontLayout'

// 后台管理界面路由：console/editor 为管理员功能，仅挂载在 /admin 下
const adminRoutes = [
    { path: 'chat', element: <ChatPage /> },
    { path: 'console', element: <ConsolePage /> },
    { path: 'editor', element: <EditorPage /> },
]

export const router = createBrowserRouter([
    {
        // 前台界面：顶部导航 + 路由内容区 + 底部页脚（仅普通用户功能）
        path: '/',
        element: <FrontLayout />,
        children: [
            { index: true, element: <Navigate to="/chat" replace /> },
            { path: 'chat', element: <ChatPage /> },
        ],
    },
    {
        // 后台管理界面：侧边栏布局
        path: '/admin',
        element: <AdminLayout />,
        children: [{ index: true, element: <Navigate to="/admin/chat" replace /> }, ...adminRoutes],
    },
])
