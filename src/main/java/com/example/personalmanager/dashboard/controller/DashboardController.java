package com.example.personalmanager.dashboard.controller;

import com.example.personalmanager.common.ApiResponse;
import com.example.personalmanager.dashboard.dto.DashboardSummaryResponse;
import com.example.personalmanager.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘控制器，提供首页汇总数据查询接口。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取仪表盘汇总数据（本月收支、计划与目标统计）。
     *
     * @return 仪表盘汇总信息
     */
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.ok(dashboardService.getSummary());
    }
}
