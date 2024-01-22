package com.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gateway.entity.MPAYLog;


@Repository
public interface MPAYLogRepository extends JpaRepository<MPAYLog, Long> {

}
