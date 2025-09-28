package com.acl.spring.app.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.acl.spring.app.backend.model.Contract;

public interface ContractRepository extends MongoRepository<Contract, String> {
    List<Contract> findByUserId(Long userId);
}