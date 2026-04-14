package com.atlas.common.mybatis.handler;

import com.atlas.common.core.annotation.DataPermission;
import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.enums.BaseEnum;
import com.atlas.common.mybatis.enums.DataScope;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/13 16:02
 */
@Slf4j
public class DataScopeHandler implements MultiDataPermissionHandler {

    private final Map<String, Optional<DataPermission>> dataPermissionCache = new ConcurrentHashMap<>();

    // 广播表/基础表黑名单：即便 alias 没配，这些表也绝不加权限逻辑
    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
            "organization", "user_role", "role_data_scope"
    ));

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        // 获取当前方法上的注解
        DataPermission dataScope = getDataScopeAnnotation(mappedStatementId);
        if (dataScope == null) {
            return null;
        }
        String currentAlias = table.getAlias() != null ? table.getAlias().getName() : "";
        String currentTableName = table.getName().toLowerCase();
        // 判定：当前表是否为目标表
        boolean isTarget = false;
        if (StringUtils.isNotBlank(dataScope.alias())) {
            // 如果显式指定了 alias，则必须严格匹配别名
            isTarget = dataScope.alias().equalsIgnoreCase(currentAlias);
        } else if (StringUtils.isNotBlank(dataScope.tableName())) {
            // 如果没写 alias 但写了 tableName，则匹配表名
            isTarget = dataScope.tableName().equalsIgnoreCase(currentTableName);
        } else {
            // 只要不是黑名单里的表，且别名为 "t" 或者干脆没有别名（单表查询），则默认视为目标表
            if (!IGNORE_TABLES.contains(currentTableName)) {
                if ("t".equalsIgnoreCase(currentAlias) || StringUtils.isBlank(currentAlias)) {
                    isTarget = true;
                }
            }

        }
        if (!isTarget) {
            return null;
        }
        // 构建权限 SQL 片段
        return buildDataScopeExpression(dataScope, table);
    }

    private Expression buildDataScopeExpression(DataPermission ds, Table table) {
        // 获取用户数据权限范围等级
        Set<String> dataScopes = UserContext.getRequiredDataScope();
        // 如果有“全部数据”权限，直接不拦截
        if (dataScopes.contains(DataScope.ALL.getCode().toString())) {
            return null;
        }
        Long userId = UserContext.getRequiredUserId();
        Long orgId = UserContext.getRequiredOrgId();
        String tableAlias = table.getAlias() != null ? table.getAlias().getName() : table.getName();

        List<String> conditions = new ArrayList<>();
        for (String scopeCode : dataScopes){
            DataScope dataScope = BaseEnum.fromCode(DataScope.class, Integer.parseInt(scopeCode));
            switch (dataScope){
                case DataScope.SELF:
                    conditions.add(String.format("%s.%s = %d", tableAlias, ds.userField(), userId));
                    break;
                case DataScope.DEPT:
                    conditions.add(String.format(
                            "EXISTS (SELECT 1 FROM organization o_target " +
                                    "INNER JOIN organization o_user ON o_user.id = %d " +
                                    "WHERE o_target.id = %s.%s " +
                                    "AND o_target.org_path LIKE CONCAT(SUBSTRING_INDEX(o_user.org_path, '/', 4), '/%%'))",
                            orgId, tableAlias, ds.orgField()));
                    break;
                case DataScope.COMPANY:
                    conditions.add(String.format(
                            "EXISTS (SELECT 1 FROM organization o_target " +
                                    "INNER JOIN organization o_user ON o_user.id = %d " +
                                    "WHERE o_target.id = %s.%s " +
                                    "AND o_target.org_path LIKE CONCAT(SUBSTRING_INDEX(o_user.org_path, '/', 3), '/%%'))",
                            orgId, tableAlias, ds.orgField()));
                    break;
                case DataScope.CUSTOM:
                    conditions.add(String.format(
                            "EXISTS (SELECT 1 FROM role_data_scope rds " +
                                    "JOIN user_role ur ON rds.role_id = ur.role_id " +
                                    "WHERE ur.user_id = %d AND rds.org_id = %s.%s)",
                            userId, tableAlias, ds.orgField()));
                    break;
            }
        }
        // 统一添加“本人可见”兜底（即便没有匹配到任何角色，用户至少能看自己的）
        String selfCondition = String.format("%s.%s = %d", tableAlias, ds.userField(), userId);
        if (conditions.stream().noneMatch(c -> c.contains(selfCondition))) {
            conditions.add(selfCondition);
        }
        String sql = "(" + String.join(" OR ", conditions) + ")";
        try {
            return StringUtils.isNotBlank(sql) ? CCJSqlParserUtil.parseCondExpression(sql) : null;
        } catch (JSQLParserException e) {
            log.error("Data scope SQL parse error: {}", sql, e);
            throw new RuntimeException("数据权限规则解析失败");
        }
    }

    // 获取 Mapper 方法或类上的注解
    private DataPermission getDataScopeAnnotation(String mappedStatementId) {
        return dataPermissionCache.computeIfAbsent(mappedStatementId, key -> {
            try {
                int lastDotIndex = key.lastIndexOf(".");
                String className = key.substring(0, lastDotIndex);
                String methodName = key.substring(lastDotIndex + 1);
                // 分页插件自动生成的 COUNT 语句
                if (methodName.endsWith("_COUNT")) {
                    methodName = methodName.substring(0, methodName.lastIndexOf("_COUNT"));
                }
                Class<?> mapperClass = Class.forName(className);
                Method[] methods = mapperClass.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals(methodName) && method.isAnnotationPresent(DataPermission.class)) {
                        return Optional.of(method.getAnnotation(DataPermission.class));
                    }
                }
                // 如果方法上没有，尝试获取类上的注解（实现全接口拦截）
                if (mapperClass.isAnnotationPresent(DataPermission.class)) {
                    return Optional.of(mapperClass.getAnnotation(DataPermission.class));
                }
            } catch (Exception e) {
                log.error("get Data scope Annotation error: ", e);
            }
            return Optional.empty();
        }).orElse(null);
    }
}
