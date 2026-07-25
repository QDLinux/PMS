package com.example.personalmanager.goal.controller;

import com.example.personalmanager.common.ApiResponse;
import com.example.personalmanager.goal.dto.GoalProgressRequest;
import com.example.personalmanager.goal.dto.GoalResponse;
import com.example.personalmanager.goal.dto.GoalRequest;
import com.example.personalmanager.goal.entity.GoalStatus;
import com.example.personalmanager.goal.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 目标管理控制器，提供目标的增删改查及进度更新接口。
 */
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    /**
     * 创建目标。
     *
     * @param request 目标创建请求
     * @return 创建后的目标详情
     */
    @PostMapping
    public ApiResponse<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        return ApiResponse.ok("创建成功", goalService.create(request));
    }

    /**
     * 更新目标。
     *
     * @param id      目标 ID
     * @param request 目标更新请求
     * @return 更新后的目标详情
     */
    @PutMapping("/{id}")
    public ApiResponse<GoalResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody GoalRequest request) {
        return ApiResponse.ok("更新成功", goalService.update(id, request));
    }

    /**
     * 为目标追加完成进度。
     *
     * @param id      目标 ID
     * @param request 进度增量请求
     * @return 更新后的目标详情
     */
    @PatchMapping("/{id}/progress")
    public ApiResponse<GoalResponse> addProgress(@PathVariable Long id,
                                                 @Valid @RequestBody GoalProgressRequest request) {
        return ApiResponse.ok("进度更新成功", goalService.addProgress(id, request.getIncrement()));
    }

    /**
     * 删除目标。
     *
     * @param id 目标 ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    /**
     * 根据 ID 查询目标详情。
     *
     * @param id 目标 ID
     * @return 目标详情
     */
    @GetMapping("/{id}")
    public ApiResponse<GoalResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(goalService.getById(id));
    }

    /**
     * 查询目标列表，可按状态筛选。
     *
     * @param status 目标状态，为空则返回全部
     * @return 目标列表
     */
    @GetMapping
    public ApiResponse<List<GoalResponse>> list(@RequestParam(required = false) GoalStatus status) {
        return ApiResponse.ok(goalService.list(status));
    }
}
