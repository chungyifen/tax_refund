package com.fox.tax;

import java.io.File;
import java.io.FileInputStream;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

public class ExcelReportInspectTest {
    @Test
    public void inspectReports() throws Exception {
        inspectFile("src/main/resources/excel/20260213172341-DBB1090370A002_A.xls", "Report A");
    }

    private void inspectFile(String path, String label) throws Exception {
        File file = new File(path);
        System.out.println("\n=== Inspecting " + label + " (" + file.getName() + ") ===");
        if (!file.exists()) {
            System.out.println("File does not exist!");
            return;
        }
        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Print first few rows to understand structure (Header + Data)
            for (int i = 0; i <= 6; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    System.out.println("Row " + i + ":");
                    for (Cell cell : row) {
                        System.out.println("  [" + cell.getAddress() + "]: " + cell.toString());
                    }
                }
            }
        }
    }
}
