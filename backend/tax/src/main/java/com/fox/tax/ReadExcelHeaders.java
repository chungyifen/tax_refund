package com.fox.tax;

import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;

public class ReadExcelHeaders {
    public static void main(String[] args) throws Exception {
        String filePath = args.length > 0 ? args[0] : "src/main/resources/excel/出口明細_DAAB09180E2001.xls";
        System.out.println("Reading file: " + filePath);
        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Print first 5 rows to identify header and data
            for (int i = 0; i <= Math.min(4, sheet.getLastRowNum()); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    System.out.println("Row " + i + ": null");
                    continue;
                }
                System.out.println("=== Row " + i + " ===");
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String val = cell == null ? "[null]" : "[" + cell.toString().trim() + "]";
                    System.out.print("  Col " + j + ": " + val);
                }
                System.out.println();
            }
        }
    }
}
