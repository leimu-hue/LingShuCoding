import { createBrowserRouter, Navigate } from 'react-router-dom'
import ChatPage from '../pages/chat/ChatPage'
import ConsolePage from '../pages/console/ConsolePage'
import EditorPage from '../pages/editor/EditorPage'
import AdminLayout from '../layouts/AdminLayout'

export const router = createBrowserRouter([
    {
        path: '/',
        element: <AdminLayout />,
        children: [
            { index: true, element: <Navigate to="/chat" replace /> },
            { path: 'chat', element: <ChatPage /> },
            { path: 'console', element: <ConsolePage /> },
            { path: 'editor', element: <EditorPage /> },
        ],
    },
])
