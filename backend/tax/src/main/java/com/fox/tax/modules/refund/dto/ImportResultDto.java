package com.fox.tax.modules.refund.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ImportResultDto {
    private int successCount = 0;
    private int errorCount = 0;
    private List<String> errorMessages = new ArrayList<>();

    public void addSuccess() {
        this.successCount++;
    }

    public void addError(String message) {
        this.errorCount++;
        this.errorMessages.add(message);
    }
}
