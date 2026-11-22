package com.budgetbuddy.api.expenses;

import java.util.List;

public record ExpenseImportPreviewResponse(List<Row> rows, int duplicateCount) {
    public record Row(String rowId, boolean duplicate, String reason) { }
}
