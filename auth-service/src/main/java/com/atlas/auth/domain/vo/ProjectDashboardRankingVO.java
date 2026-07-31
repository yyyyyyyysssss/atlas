package com.atlas.auth.domain.vo;

public record ProjectDashboardRankingVO(

        String applicationName,

        Long score

) {

    public static ProjectDashboardRankingVO of(String applicationName, Long score) {
        return new ProjectDashboardRankingVO(applicationName, score);
    }

}
