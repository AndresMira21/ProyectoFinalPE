package com.acl.spring.app.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.acl.spring.app.backend.dto.AnalysisDatos;
import com.acl.spring.app.backend.model.Contract;
import com.acl.spring.app.backend.repository.ContractRepository;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final NLPAnalysisService nlpAnalysisService;

    public ContractService(ContractRepository contractRepository, NLPAnalysisService nlpAnalysisService) {
        this.contractRepository = contractRepository;
        this.nlpAnalysisService = nlpAnalysisService;
    }

    public Contract saveWithAnalysis(String name, String content, Long userId) {
        AnalysisDatos.AnalysisResult analysis = nlpAnalysisService.analyze(content);

        Contract c = new Contract();
        c.setName(name);
        c.setContent(content);
        c.setType(analysis.getType());
        c.setKeyClauses(analysis.getKeyClauses());
        c.setRisks(analysis.getRisks());
        c.setRiskScore(analysis.getRiskScore());
        c.setUserId(userId);

        return contractRepository.save(c);
    }

    public Optional<Contract> findById(String id) {
        return contractRepository.findById(id);
    }

    public List<Contract> listAll() {
        return contractRepository.findAll();
    }

    public List<Contract> listByUser(Long userId) {
        return contractRepository.findByUserId(userId);
    }
}      
