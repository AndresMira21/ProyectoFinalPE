package com.acl.spring.app.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.acl.spring.app.backend.dto.AnalysisDatos;
import com.acl.spring.app.backend.model.Contract;
import com.acl.spring.app.backend.service.ContractService;
import com.acl.spring.app.backend.service.NLPAnalysisService;
import com.acl.spring.app.backend.service.ReportService;
import com.acl.spring.app.backend.service.TextExtractionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final TextExtractionService textExtractionService;
    private final NLPAnalysisService nlpAnalysisService;
    private final ContractService contractService;
    private final ReportService reportService;

    public ContractController(TextExtractionService textExtractionService,
                              NLPAnalysisService nlpAnalysisService,
                              ContractService contractService,
                              ReportService reportService) {
        this.textExtractionService = textExtractionService;
        this.nlpAnalysisService = nlpAnalysisService;
        this.contractService = contractService;
        this.reportService = reportService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisDatos.UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @AuthenticationPrincipal UserDetails user
    ) throws Exception {
        String text = textExtractionService.extractText(file);
        String contractName = name != null ? name : file.getOriginalFilename();

        Long userId = null;
        if (user != null) {
            // en un caso real, buscaríamos el id del usuario; aquí dejamos null si no está disponible
        }

        Contract saved = contractService.saveWithAnalysis(contractName, text, userId);
        AnalysisDatos.AnalysisResult analysis = nlpAnalysisService.analyze(text);

        AnalysisDatos.UploadResponse resp = new AnalysisDatos.UploadResponse();
        resp.setContractId(saved.getId());
        resp.setAnalysis(analysis);
        return ResponseEntity.ok(resp);
    }

    @PostMapping(value = "/analyze-text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisDatos.AnalysisResult> analyzeText(@Valid @RequestBody AnalysisDatos.AnalyzeTextRequest req) {
        AnalysisDatos.AnalysisResult result = nlpAnalysisService.analyze(req.getText());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<Contract>> listAll() {
        return ResponseEntity.ok(contractService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getById(@PathVariable String id) {
        return contractService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> getReport(@PathVariable String id) throws Exception {
        var contract = contractService.findById(id).orElse(null);
        if (contract == null) return ResponseEntity.notFound().build();

        var analysis = nlpAnalysisService.analyze(contract.getContent());
        byte[] pdf = reportService.generatePdf(contract, analysis);

        String filename = URLEncoder.encode("reporte-" + contract.getName() + ".pdf", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/{id}/qa")
    public ResponseEntity<List<String>> qa(
            @PathVariable String id,
            @RequestParam("q") String question
    ) {
        var contract = contractService.findById(id).orElse(null);
        if (contract == null) return ResponseEntity.notFound().build();
        var answers = nlpAnalysisService.answerQuestions(contract.getContent(), question);
        return ResponseEntity.ok(answers);
    }
}
