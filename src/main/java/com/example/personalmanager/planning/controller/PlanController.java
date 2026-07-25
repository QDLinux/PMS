package com.example.personalmanager.planning.controller;

import com.example.personalmanager.common.ApiResponse;
import com.example.personalmanager.planning.dto.PlanItemRequest;
import com.example.personalmanager.planning.dto.PlanItemResponse;
import com.example.personalmanager.planning.entity.PlanStatus;
import com.example.personalmanager.planning.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计划项 REST 控制器，提供计划的增删改查接口。
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /**
     * 创建一项计划。
     *
     * @param request 计划请求数据
     * @return 创建后的计划
     */
    @PostMapping
    public ApiResponse<PlanItemResponse> create(@Valid @RequestBody PlanItemRequest request) {
        return ApiResponse.ok("创建成功", planService.create(request));
    }

    /**
     * 更新指定计划。
     *
     * @param id      计划ID
     * @param request 计划请求数据
     * @return 更新后的计划
     */
    @PutMapping("/{id}")
    public ApiResponse<PlanItemResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody PlanItemRequest request) {
        return ApiResponse.ok("更新成功", planService.update(id, request));
    }

    /**
     * 删除指定计划。
     *
     * @param id 计划ID
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    /**
     * 根据ID查询计划。
     *
     * @param id 计划ID
     * @return 对应的计划
     */
    @GetMapping("/{id}")
    public ApiResponse<PlanItemResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(planService.getById(id));
    }

    /**
     * 查询计划列表，可按状态筛选。
     *
     * @param status 计划状态，可空（为空时返回全部）
     * @return 计划列表
     */
    @GetMapping
    public ApiResponse<List<PlanItemResponse>> list(@RequestParam(required = false) PlanStatus status) {
        return ApiResponse.ok(planService.list(status));
    }
}
