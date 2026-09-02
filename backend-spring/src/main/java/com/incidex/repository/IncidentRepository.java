package com.incidex.repository;

import com.incidex.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    // 基础的 save(), findById(), findAll(), deleteById() 等增删改查方法
    // JPA 已经在后台自动帮我们实现好了，一行 SQL 都不用写！
}