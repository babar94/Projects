package com.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.MPAYLog;



public interface MPAYLogRepository extends JpaRepository<MPAYLog, Long> {

}
