package com.grad.ancienttree.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller 只读。
 * 使用 ThreadLocal 保证同一请求线程内可取到角色与数据范围。
 */
public class UserContext {

    /**
     * dataScope 取值：ALL=全部数据、ORG=本部门、SELF=本人负责的数据。
     * 需求文档第 100 行：普通业务人员仅查看本人负责或被共享的数据，部门负责人看本部门，管理员按组织范围。
     */
    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, String dataScope) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.realName();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long orgId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.orgId();
    }

    public static String dataScope() {
        CurrentUser u = HOLDER.get();
        return u == null ? "SELF" : u.dataScope();
    }

    /** 巡查人员只能录入自己任务范围内的记录 */
    public static boolean isPatrol() {
        return "patrol".equals(roleCode());
    }

    /** 养护单位只能处理派给本单位的工单 */
    public static boolean isCareUnit() {
        return "care".equals(roleCode());
    }

    /** 园林专家负责方案审核与健康评估 */
    public static boolean isExpert() {
        return "expert".equals(roleCode());
    }

    /** 主管部门管理员拥有全量数据范围 */
    public static boolean isAdmin() {
        return "admin".equals(roleCode());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
