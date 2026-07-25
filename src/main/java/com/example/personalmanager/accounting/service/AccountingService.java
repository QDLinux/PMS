package com.example.personalmanager.accounting.service;

import com.example.personalmanager.accounting.dto.AccountingRecordRequest;
import com.example.personalmanager.accounting.entity.AccountingRecord;
import com.example.personalmanager.accounting.repository.AccountingRecordRepository;
import com.example.personalmanager.auth.service.CurrentUserService;
import com.example.personalmanager.common.BusinessException;
import com.example.personalmanager.common.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 记账业务服务，处理记账记录的创建、更新、删除及查询逻辑，并保证仅操作当前用户的数据。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingService {

    private final AccountingRecordRepository recordRepository;
    private final CurrentUserService currentUserService;

    /**
     * 创建记账记录，归属于当前登录用户。
     *
     * @param request 记账记录请求数据
     * @return 已保存的记账记录
     */
    @Transactional
    public AccountingRecord create(AccountingRecordRequest request) {
        AccountingRecord record = new AccountingRecord();
        record.setOwnerId(currentUserService.getCurrentUserId());
        apply(record, request);
        return recordRepository.save(record);
    }

    /**
     * 更新指定记账记录。
     *
     * @param id      记录ID
     * @param request 记账记录请求数据
     * @return 更新后的记账记录
     */
    @Transactional
    public AccountingRecord update(Long id, AccountingRecordRequest request) {
        AccountingRecord record = getById(id);
        apply(record, request);
        return recordRepository.save(record);
    }

    /**
     * 删除指定记账记录。
     *
     * @param id 记录ID
     */
    @Transactional
    public void delete(Long id) {
        AccountingRecord record = getById(id);
        recordRepository.delete(record);
    }

    /**
     * 根据ID查询当前用户的记账记录，不存在时抛出业务异常。
     *
     * @param id 记录ID
     * @return 对应的记账记录
     */
    public AccountingRecord getById(Long id) {
        Long ownerId = currentUserService.getCurrentUserId();
        return recordRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new BusinessException("记账记录不存在, id=" + id));
    }

    /**
     * 查询当前用户的全部记账记录。
     *
     * @return 记账记录列表
     */
    public List<AccountingRecord> listAll() {
        return recordRepository.findAllByOwnerIdOrderByAccountDateDescIdDesc(currentUserService.getCurrentUserId());
    }

    /**
     * 按日期区间查询当前用户的记账记录。
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 区间内的记账记录列表
     */
    public List<AccountingRecord> listByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        // 将日期区间扩展为当天起始至结束当天最后时刻，保证边界完整覆盖
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return recordRepository.findAllByOwnerIdAndAccountDateBetweenOrderByAccountDateDescIdDesc(
                currentUserService.getCurrentUserId(), startDateTime, endDateTime);
    }

    // 将请求数据应用到实体：校验日期并填充各字段
    private void apply(AccountingRecord record, AccountingRecordRequest request) {
        if (request.getAccountDate() != null && request.getAccountDate().isAfter(LocalDateTime.now())) {
            throw new BusinessException("记账日期不能晚于当前时间");
        }
        record.setType(request.getType());
        record.setAmount(request.getAmount());
        record.setCategory(request.getCategory().trim());
        record.setAccountDate(request.getAccountDate());
        record.setNote(TextUtils.normalizeNullable(request.getNote()));
    }
}
