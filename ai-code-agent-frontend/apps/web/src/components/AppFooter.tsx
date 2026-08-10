import { Layout, theme } from 'antd'

const { Footer } = Layout

/**
 * 站点页脚：版权信息栏。
 * 位置始终固定于页面底部（由外层 Layout 的 flex 布局保证，内容区滚动）。
 */
export default function AppFooter() {
    const { token } = theme.useToken()

    return (
        <Footer
            style={{
                padding: '16px 24px',
                textAlign: 'center',
                background: token.colorBgContainer,
                borderTop: `1px solid ${token.colorBorderSecondary}`,
                color: token.colorTextSecondary,
            }}
        >
            AI 零代码应用生成平台
        </Footer>
    )
}
