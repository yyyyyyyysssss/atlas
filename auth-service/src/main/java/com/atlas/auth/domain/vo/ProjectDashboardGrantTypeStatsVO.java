package com.atlas.auth.domain.vo;

import java.util.List;

public record ProjectDashboardGrantTypeStatsVO(
        String applicationName,
        List<GrantTypeItem> grantTypes
) {

    public record GrantTypeItem(
            String applicationName,
            String grantType,
            Long count
    ) {
        public static GrantTypeItem of(String applicationName, String grantType, Long count) {
            return new GrantTypeItem(applicationName, grantType, count);
        }
    }

    public static ProjectDashboardGrantTypeStatsVO of(String applicationName, List<GrantTypeItem> grantTypes) {
        return new ProjectDashboardGrantTypeStatsVO(applicationName, grantTypes);
    }
}