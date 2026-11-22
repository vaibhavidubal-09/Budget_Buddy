package com.budgetbuddy.api.expenses;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/expenses")
public class TransactionImageImportController {

    private final TransactionImageImportService transactionImageImportService;

    public TransactionImageImportController(TransactionImageImportService transactionImageImportService) {
        this.transactionImageImportService = transactionImageImportService;
    }

    @PostMapping({"/import-image", "/import-file"})
    public List<ExpenseUnderstandingResponse> importDocument(@RequestParam("file") MultipartFile file) {
        return transactionImageImportService.importDocument(file);
    }
}
