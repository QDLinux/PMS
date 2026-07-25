package com.example.personalmanager.goal.service;

import com.example.personalmanager.auth.service.CurrentUserService;
import com.example.personalmanager.common.BusinessException;
import com.example.personalmanager.common.TextUtils;
import com.example.personalmanager.goal.dto.GoalResponse;
import com.example.personalmanager.goal.dto.GoalRequest;
import com.example.personalmanager.goal.entity.Goal;
import com.example.personalmanager.goal.entity.GoalStatus;
import com.example.personalmanager.goal.repository.GoalRepository;
import com.example.personalmanager.goal.support.GoalStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 目标业务服务，负责目标的创建、更新、进度累加、查询与删除，
 * 并在持久化前后通过 {@link GoalStatusResolver} 推导目标状态。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final CurrentUserService currentUserService;
    private final GoalStatusResolver goalStatusResolver;

    /**
     * 创建目标。
     *
     * @param request 目标创建请求
     * @return 创建后的目标详情
     */
    @Transactional
    public GoalResponse create(GoalRequest request) {
        validateRequest(request);
        Goal goal = new Goal();
        goal.setOwnerId(currentUserService.getCurrentUserId());
        apply(goal, request);
        return toResponse(goalRepository.save(goal), LocalDate.now());
    }

    /**
     * 更新指定目标。
     *
     * @param id      目标 ID
     * @param request 目标更新请求
     * @return 更新后的目标详情
     */
    @Transactional
    public GoalResponse update(Long id, GoalRequest request) {
        validateRequest(request);
        Goal goal = getGoalEntityById(id);
        apply(goal, request);
        return toResponse(goalRepository.save(goal), LocalDate.now());
    }

    /**
     * 为目标累加完成金额，并重新推导目标状态。
     *
     * @param id        目标 ID
     * @param increment 进度增量
     * @return 更新后的目标详情
     */
    @Transactional
    public GoalResponse addProgress(Long id, BigDecimal increment) {
        Goal goal = getGoalEntityById(id);
        goal.setCurrentAmount(goal.getCurrentAmount().add(increment));
        goal.setStatus(goalStatusResolver.resolve(goal.getCurrentAmount(), goal.getTargetAmount(), goal.getStartDate(), goal.getDeadline(), LocalDate.now()));
        return toResponse(goalRepository.save(goal), LocalDate.now());
    }

    /**
     * 删除指定目标。
     *
     * @param id 目标 ID
     */
    @Transactional
    public void delete(Long id) {
        Goal goal = getGoalEntityById(id);
        goalRepository.delete(goal);
    }

    /**
     * 根据 ID 查询目标详情。
     *
     * @param id 目标 ID
     * @return 目标详情
     */
    public GoalResponse getById(Long id) {
        return toResponse(getGoalEntityById(id), LocalDate.now());
    }

    /**
     * 查询当前用户的目标列表，可按状态筛选。
     *
     * @param status 目标状态，为空则不筛选
     * @return 目标列表
     */
    public List<GoalResponse> list(GoalStatus status) {
        Long ownerId = currentUserService.getCurrentUserId();
        List<Goal> goals = goalRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        LocalDate today = LocalDate.now();
        return goals.stream()
                .map(goal -> toResponse(goal, today))
                .filter(goal -> status == null || goal.getStatus() == status)
                .toList();
    }

    // 将请求数据应用到目标实体，并推导目标状态
    private void apply(Goal goal, GoalRequest request) {
        goal.setName(request.getName().trim());
        goal.setDescription(TextUtils.normalizeNullable(request.getDescription()));
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(request.getCurrentAmount());
        goal.setStartDate(request.getStartDate());
        goal.setDeadline(request.getDeadline());
        goal.setStatus(goalStatusResolver.resolve(
                request.getCurrentAmount(),
                request.getTargetAmount(),
                request.getStartDate(),
                request.getDeadline(),
                LocalDate.now()
        ));
    }

    // 校验请求合法性：当前金额不超过目标金额、开始日期不晚于截止日期
    private void validateRequest(GoalRequest request) {
        if (request.getCurrentAmount().compareTo(request.getTargetAmount()) > 0) {
            throw new BusinessException("当前金额不能超过目标金额");
        }
        if (request.getStartDate() != null
                && request.getDeadline() != null
                && request.getStartDate().isAfter(request.getDeadline())) {
            throw new BusinessException("开始日期不能晚于截止日期");
        }
    }

    // 按 ID 和当前用户查询目标实体，不存在则抛出业务异常
    private Goal getGoalEntityById(Long id) {
        Long ownerId = currentUserService.getCurrentUserId();
        return goalRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new BusinessException("未找到目标，id=" + id));
    }

    // 将目标实体转换为响应 DTO，并基于指定日期实时推导状态
    private GoalResponse toResponse(Goal goal, LocalDate today) {
        GoalStatus status = goalStatusResolver.resolve(goal.getCurrentAmount(), goal.getTargetAmount(), goal.getStartDate(), goal.getDeadline(), today);
        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .description(goal.getDescription())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .startDate(goal.getStartDate())
                .deadline(goal.getDeadline())
                .status(status)
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
