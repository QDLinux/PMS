package com.example.personalmanager.accounting.repository;

import com.example.personalmanager.accounting.entity.AccountingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 记账记录数据访问接口。
 */
public interface AccountingRecordRepository extends JpaRepository<AccountingRecord, Long> {

    // 按ID和所属用户查询单条记录，保证数据隔离
    Optional<AccountingRecord> findByIdAndOwnerId(Long id, Long ownerId);

    // 查询某用户全部记录，按记账日期、ID倒序
    List<AccountingRecord> findAllByOwnerIdOrderByAccountDateDescIdDesc(Long ownerId);

    // 查询某用户指定日期区间内的记录，按记账日期、ID倒序
    List<AccountingRecord> findAllByOwnerIdAndAccountDateBetweenOrderByAccountDateDescIdDesc(Long ownerId,
                                                                                              LocalDateTime startDate,
                                                                                              LocalDateTime endDate);
}
