package com.example.personalmanager.planning.service;

import com.example.personalmanager.auth.service.CurrentUserService;
import com.example.personalmanager.common.BusinessException;
import com.example.personalmanager.common.TextUtils;
import com.example.personalmanager.planning.dto.PlanItemRequest;
import com.example.personalmanager.planning.dto.PlanItemResponse;
import com.example.personalmanager.planning.entity.PlanItem;
import com.example.personalmanager.planning.entity.PlanStatus;
import com.example.personalmanager.planning.repository.PlanItemRepository;
import com.example.personalmanager.planning.support.PlanStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 计划业务服务，处理计划的创建、更新、删除及查询，并在读写时通过 PlanStatusResolver 推导计划状态。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanItemRepository planItemRepository;
    private final CurrentUserService currentUserService;
    private final PlanStatusResolver planStatusResolver;

    /**
     * 创建计划，归属于当前登录用户。
     *
     * @param request 计划请求数据
     * @return 创建后的计划响应
     */
    @Transactional
    public PlanItemResponse create(PlanItemRequest request) {
        validateDateRange(request);
        PlanItem item = new PlanItem();
        item.setOwnerId(currentUserService.getCurrentUserId());
        apply(item, request);
        return toResponse(planItemRepository.save(item), LocalDate.now());
    }

    /**
     * 更新指定计划。
     *
     * @param id      计划ID
     * @param request 计划请求数据
     * @return 更新后的计划响应
     */
    @Transactional
    public PlanItemResponse update(Long id, PlanItemRequest request) {
        validateDateRange(request);
        PlanItem item = getPlanEntityById(id);
        apply(item, request);
        return toResponse(planItemRepository.save(item), LocalDate.now());
    }

    /**
     * 删除指定计划。
     *
     * @param id 计划ID
     */
    @Transactional
    public void delete(Long id) {
        PlanItem item = getPlanEntityById(id);
        planItemRepository.delete(item);
    }

    /**
     * 根据ID查询计划详情，状态按当前日期推导。
     *
     * @param id 计划ID
     * @return 计划响应
     */
    public PlanItemResponse getById(Long id) {
        return toResponse(getPlanEntityById(id), LocalDate.now());
    }

    /**
     * 查询当前用户的计划列表，可按状态筛选。
     *
     * @param status 计划状态，为空时返回全部
     * @return 计划响应列表
     */
    public List<PlanItemResponse> list(PlanStatus status) {
        Long ownerId = currentUserService.getCurrentUserId();
        List<PlanItem> items = planItemRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        LocalDate today = LocalDate.now();
        // 先按当前日期推导状态，再按目标状态过滤
        return items.stream()
                .map(item -> toResponse(item, today))
                .filter(item -> status == null || item.getStatus() == status)
                .toList();
    }

    // 将请求数据应用到实体，并通过状态推导器计算最终状态
    private void apply(PlanItem item, PlanItemRequest request) {
        item.setTitle(request.getTitle().trim());
        item.setDescription(TextUtils.normalizeNullable(request.getDescription()));
        item.setStartDate(request.getStartDate());
        item.setEndDate(request.getEndDate());
        item.setPriority(request.getPriority());
        item.setStatus(planStatusResolver.resolve(request.getStatus(), request.getStartDate(), request.getEndDate(), LocalDate.now()));
    }

    // 校验开始日期不晚于结束日期
    private void validateDateRange(PlanItemRequest request) {
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }

    // 根据ID查询当前用户的计划实体，不存在时抛出业务异常
    private PlanItem getPlanEntityById(Long id) {
        Long ownerId = currentUserService.getCurrentUserId();
        return planItemRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new BusinessException("未找到规划，id=" + id));
    }

    // 将实体转换为响应对象，状态按传入日期重新推导
    private PlanItemResponse toResponse(PlanItem item, LocalDate today) {
        PlanStatus status = planStatusResolver.resolve(item.getStatus(), item.getStartDate(), item.getEndDate(), today);
        return PlanItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .priority(item.getPriority())
                .status(status)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
