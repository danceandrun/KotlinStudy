package com.yongfeng.aps

import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File

class ExcelReader {
    fun readExcel(filePath: String): List<PieceStep> {
        val pieces = mutableListOf<PieceStep>()
        val workbook = WorkbookFactory.create(File(filePath))
        val sheet = workbook.getSheetAt(0)

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex)
            val obj = PieceStep()
            obj.pieceStepNr = row.getCell(0).stringCellValue
            obj.sequence = row.getCell(1).numericCellValue.toInt()
            obj.width = row.getCell(2).numericCellValue
            obj.thickness = row.getCell(3).numericCellValue
            obj.innerSteelGrade = row.getCell(4).stringCellValue
            obj.steelGrade = row.getCell(5).stringCellValue
            obj.minThickForNext = row.getCell(6).numericCellValue
            obj.maxThickForNext = row.getCell(7).numericCellValue
            obj.nextMaxThick = row.getCell(8).numericCellValue
            obj.nextMinThick = row.getCell(9).numericCellValue
            pieces.add(obj)
        }
        workbook.close()
        return pieces
    }
}