package com.learnspring.checkinserver.repository;

import com.learnspring.checkinserver.model.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {
    boolean existsByTaskIdAndDate(Long taskId, LocalDate date);
    Optional<TaskLog> findByTaskIdAndDate(Long taskId, LocalDate date);
    List<TaskLog> findByTaskId(Long taskId);

    @Transactional
    void deleteByTaskIdAndDate(Long taskId, LocalDate date);
}
