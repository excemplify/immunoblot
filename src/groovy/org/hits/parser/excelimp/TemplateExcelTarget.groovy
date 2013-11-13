/* ===================================================
 * Copyright 2010-2013 HITS gGmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ========================================================== */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hits.parser.excelimp

import org.hits.parser.core.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

import org.hits.parser.TargetDef
/**
 *
 * @author jongle
 */
class TemplateExcelTarget implements Target{
	
    String targetString
    CellRangeAddress targetCRA
    Sheet sheet
    int rowDiff
    int colDiff
    int firstRow,firstColumn,lastRow,lastColumn
    int size
    Workbook template
    
    
   public TemplateExcelTarget(TargetDef targetDef, StateAndQueue state){
       
        
        InputStream is = new ByteArrayInputStream(targetDef.template.binaryFileData)
        this.template = WorkbookFactory.create(is)
        this.targetCRA=CellRangeAddress.valueOf(targetDef.cellRange)
        this.sheet=template.getSheetAt(targetDef.sheetNum)
        this.firstRow=targetCRA.getFirstRow()
        this.firstColumn=targetCRA.getFirstColumn()
        this.lastColumn=targetCRA.getLastColumn()
        this.lastRow=targetCRA.getLastRow()
        this.targetString=targetDef.cellRange
        this.size=targetCRA.getNumberOfCells()
        println "gettemplate target"
   }
    
   

    def setColDiff(int sourceFirstCol){
        this.colDiff = firstColumn-sourceFirstCol
    }
    
    def setRowDiff(int sourceFirstRow){
        this.rowDiff = firstRow-sourceFirstRow
    }
    
     def getSize(){
         return this.size
     }
     
   def resetTarget(Map configs){
       println "resetting target"
       if (configs.firstColumn) {
           this.targetCRA.setFirstColumn(configs.firstColumn)
           this.firstColumn=configs.firstColumn
       }
       if (configs.lastColumn) {
           this.targetCRA.setLastColumn(configs.lastColumn)
           this.lastColumn=configs.lastColumn
       }
       if (configs.firstRow) {
           this.targetCRA.setFirstRow(configs.firstRow)
           this.firstRow=configs.firstRow
       }
       if (configs.lastRow) {
           this.targetCRA.setLastRow(configs.lastRow)
           this.lastRow=configs.lastRow
       }
       this.targetString=targetCRA.formatAsString()
       this.size=targetCRA.getNumberOfCells()
       println "target $targetString"
        
       
   }
   
    def findLastColumnAndReset(int headerRowNumber){
       Row headers= sheet.getRow(headerRowNumber)
       int lastFilledColumn = headers.getLastCellNum()
       this.resetTarget(firstColumn:lastFilledColumn, lastColumn:lastFilledColumn)
    }
}

