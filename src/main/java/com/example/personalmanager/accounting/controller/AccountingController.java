package com.example.personalmanager.accounting.controller;

import com.example.personalmanager.accounting.dto.AccountingRecordRequest;
import com.example.personalmanager.accounting.entity.AccountingRecord;
import com.example.personalmanager.accounting.service.AccountingService;
import com.example.personalmanager.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 记账记录 REST 控制器，提供记账记录的增删改查接口。
 */
@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;

    /**
     * 创建一条记账记录。
     *
     * @param request 记账记录请求数据
     * @return 创建后的记账记录
     */
    @PostMapping
    public ApiResponse<AccountingRecord> create(@Valid @RequestBody AccountingRecordRequest request) {
        return ApiResponse.ok("创建成功", accountingService.create(request));
    }

    /**
     * 更新指定记账记录。
     *
     * @param id      记录ID
     * @param request 记账记录请求数据
     * @return 更新后的记账记录
     */
    @PutMapping("/{id}")
    public ApiResponse<AccountingRecord> update(@PathVariable Long id,
                                                @Valid @RequestBody AccountingRecordRequest request) {
        return ApiResponse.ok("更新成功", accountingService.update(id, request));
    }

    /**
     * 删除指定记账记录。
     *
     * @param id 记录ID
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        accountingService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    /**
     * 根据ID查询记账记录。
     *
     * @param id 记录ID
     * @return 对应的记账记录
     */
    @GetMapping("/{id}")
    public ApiResponse<AccountingRecord> getById(@PathVariable Long id) {
        return ApiResponse.ok(accountingService.getById(id));
    }

    /**
     * 查询记账记录列表。传入起止日期时按日期区间查询，否则返回全部记录。
     *
     * @param startDate 起始日期，可空
     * @param endDate   结束日期，可空
     * @return 记账记录列表
     */
    @GetMapping
    public ApiResponse<List<AccountingRecord>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // 起止日期均提供时按区间查询，否则查询全部
        if (startDate != null && endDate != null) {
            return ApiResponse.ok(accountingService.listByDateRange(startDate, endDate));
        }
        return ApiResponse.ok(accountingService.listAll());
    }
}
