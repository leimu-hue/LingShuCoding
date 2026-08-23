package com.dp.ai_code_agent.user.spi.context;

import com.dp.ai_code_agent.user.spi.model.UserIdentity;

/**
 * 当前请求的用户上下文（零框架依赖，仅 JDK）。
 * <p>
 * 基于 {@link ScopedValue} 而非 ThreadLocal：作用域退出自动解绑，
 * 杜绝 servlet 线程池复用导致的串号/泄漏。由 web 层在请求边界绑定。
 */
public final class UserContext {

    private static final ScopedValue<UserIdentity> CURRENT = ScopedValue.newInstance();

    private UserContext() {
    }

    /** 当前登录用户身份；未绑定时返回 {@code null}。 */
    public static UserIdentity get() {
        return CURRENT.isBound() ? CURRENT.get() : null;
    }

    /** 当前用户 ID；未绑定时返回 {@code null}。 */
    public static Long getUserId() {
        UserIdentity identity = get();
        return identity == null ? null : identity.id();
    }

    /** 当前用户名；未绑定时返回 {@code null}。 */
    public static String getUsername() {
        UserIdentity identity = get();
        return identity == null ? null : identity.username();
    }

    /** 在给定身份作用域内执行 {@code op}，作用域退出后自动解绑。 */
    public static <R, X extends Throwable> R scoped(UserIdentity identity,
                                                    ScopedValue.CallableOp<R, X> op) throws X {
        return ScopedValue.where(CURRENT, identity).call(op);
    }
}
