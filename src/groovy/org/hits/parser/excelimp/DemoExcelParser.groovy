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

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory


import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Color
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.poifs.filesystem.POIFSFileSystem

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Font
/**
 *
 * @author jongle
 */
class DemoExcelParser implements Parser{

    Source source
    Target target
    Action action
    String name
    final static Set rowWiseActions=["MERGE_COLUMNS","SPLIT_COLUMNS,ADD_ROW_DATA"]
    final static Set noSourceActions=["ADD_ROW_DATA"]
    
   // int sheetnum
     def blankcells
    Sheet sheet
    
    def parsingResults=[:]
    def actionExtras=[:]
    
    public StateAndQueue parse(StateAndQueue state) throws ParsingException{
        
        
        println "parsing spreadsheet now...${name}"
        if (rowWiseActions.contains(action.getAction())){
            sheet=source.traverseRowise(doAction)
        }
        else if (noSourceActions.contains(action.getAction())){
            //no actual parsing going on here but more processing. Better would be to define headers
            //when defining a template
            println state.state.targetsheet
            sheet=makeDemoHeaders(state.state.targetsheet)
        }
        else{
            sheet=source.traverse(doAction)
        }
        
        
        //state.state.sheet=sheet
        sheet=state.state.targetsheet
       
        //need to reconfigure the 
       
        
            state.state.blankcells=blankcells
            byte[] parsedBook
            def outstream = new ByteArrayOutputStream()
            sheet.getWorkbook().write(outstream)
            parsedBook=outstream.toByteArray()
            state.state.parsedFile= parsedBook
            state.state.success=true
            return state
        
        
        
    }
    
    
    public setSource(Source source){
        this.source=source
    }
    
    public setAction(Action action){
        this.action=action
    }
    
    public setTarget(Target target){
        this.target=target
    }
    
    public configure(StateAndQueue state, Map configurations) throws ParserConfigException{
        
        println "configuring parser...${name}"
       
       // state.state.sheetnum=sheetnum
        //this.sheet = state.state.sheet
       
            //get the file from the session, read it and get the current sheet number
            File file = state.state.file
           
           
            Workbook wbook = WorkbookFactory.create(file)
            if (configurations.sheetNumber){
                println configurations.sheetNumber
                this.sheet=wbook.getSheetAt(configurations.sheetNumber as int)
            }
            else this.sheet=wbook.getSheetAt(wbook.getNumberOfSheets()-1)
            state.state.sheet=sheet
           
      //  }
      //  catch(IOException ex){
            
      //  }
      
        if (state.state.newsource){
            this.source=new ExcelSource(state.state.newsource, state)
        }
        else this.source=new ExpandingExcelSource(state.state.source, state)
        println "source in state $state.state.source"
        println "newsource in state $state.state.newsource"
        if (state.state.targetsheet){
            def firstTargetCol = source.getLastColumn()+1
            def colNumbers = source.getLastColumn()-source.getFirstColumn()
            def lastRowNum= source.getLastRow()
            def  firstRowNum=source.getFirstRow()
            def lastColumnNum=firstTargetCol+colNumbers
            this.target = new ExcelTarget([firstRow:firstRowNum, lastRow:lastRowNum, firstColumn:firstTargetCol,
                    lastColumn:lastColumnNum],state)
            state.state.newsource=target.getTargetString()
        }
        else{
            this.target = new ExcelTarget(configurations.target,state)
            //temporary
            def numtargetCols=1
            target.resetTarget([lastRow:source.getLastRow()-source.getFirstRow() ,lastColumn:target.getLastColumn()+numtargetCols-1])
            state.state.newsource=target.getTargetString()
        }
        target.setRowDiff(source.getFirstRow())
        target.setColDiff(source.getFirstColumn())
        this.action=ImmunoParserAction(configurations.action)
        doActionExtras()
        //checks
        checkConfig()
        //this.errorHandler = configurations.errorHandler
       return state  
    }
    
    
    def checkConfig() throws ParserConfigException{
        println "checking config"
        if (action.action==ImmunoParserAction.COPY || action.action==ImmunoParserAction.TRANSPOSE){
            
            if (source.size!=target.size){
                println "source: ${source.size} target:${target.size}" 
                throw new ParserConfigException("Source and Target mismatch, please check parser configuration")
            }
            
        }
    }
    
    def parseCell(Cell cell){
   
       switch (cell.getCellType()) {
           case cell.CELL_TYPE_STRING : 
            return cell.getStringCellValue()
           case cell.CELL_TYPE_NUMERIC :
           return cell.getNumericCellValue()
           case cell.CELL_TYPE_BOOLEAN :
           return cell.getBooleanCellValue()
           case cell.CELL_TYPE_FORMULA :
           return cell.getCellFormula()
           case cell.CELL_TYPE_ERROR :
           return cell.getErrorCellValue()
           case cell.CELL_TYPE_BLANK:  
                blankcells = true
                defaultErrorHandler(cell)
                //cell.setCellStyle(colourCell)           
                return ""
           
       }
}

  
    // default error handler will turn cell blue if blank or other error
    def defaultErrorHandler={Cell cell->
        Workbook wbook = cell.getSheet().getWorkbook()
        CellStyle colourCell = wbook.createCellStyle()
        colourCell.setFillForegroundColor(HSSFColor.AQUA.index)
        colourCell.setFillPattern(CellStyle.SOLID_FOREGROUND);
        
        cell.setCellStyle(colourCell)
        
    }
    
    def doAction={cells->
        
        switch (action.getAction()) {
           case ImmunoParserAction.TRANSPOSE : 
                  transpose(cells)
                  break
           case   ImmunoParserAction.COPY:
                 copy(cells) 
                 break
           case   ImmunoParserAction.MERGE_COLUMNS:
                 merge(cells)
                 break
           case ImmunoParserAction.SPLIT_COLUMNS:
                 split(cells)
                 break
           case ImmunoParserAction.ADD_COLUMN_DATA:
                 addColumnData(cells)
                 break
                 
        }
    }
    
    def transpose={Cell cell ->
        Sheet sheet = target.sheet
        int transrownum=cell.getColumnIndex()
        int transcolnum=cell.getRowIndex()
       
        Row row = sheet.getRow(transrownum+target.firstRow-source.firstColumn)
        if (row==null){
            
            row=sheet.createRow(transrownum+target.firstRow-source.firstColumn)
        }
        Cell targetCell=row.createCell(cell.getRowIndex()+target.firstColumn-source.firstRow)
        targetCell.setCellValue(parseCell(cell))
    }
    
    def copy={Cell cell ->
        Sheet sheet = target.sheet
       
        Row row = sheet.getRow(cell.getRowIndex()+target.rowDiff)
        if (row==null){
            
            row=sheet.createRow(cell.getRowIndex()+target.rowDiff)
        }
        Cell targetCell=row.createCell(cell.getColumnIndex()+target.colDiff)
        targetCell.setCellValue(parseCell(cell))
    }
    
    def merge={List<Cell> cells->
        //column difference measured from cell1
        Sheet targetSheet = target.sheet
        
        Row row = targetSheet.getRow(cells.first().getRowIndex()+target.rowDiff)
        if (row==null){
            
            row=targetSheet.createRow(cells.first().getRowIndex()+target.rowDiff)
        }
        Cell targetCell=row.createCell(cells.first().getColumnIndex()+target.colDiff)
        String targetCellVal=""
        cells.each{ targetCellVal="${targetCellVal}${parseCell(it)}"}
        println targetCellVal
        targetCell.setCellValue(targetCellVal)
    
    }
    
    def split={Cell cell, String regexp->
       Sheet targetSheet = target.sheet
        
        Row row = targetSheet.getRow(cells.first().getRowIndex()+target.rowDiff)
        if (row==null){
            
            row=targetSheet.createRow(cell.first().getRowIndex()+target.rowDiff)
        }
        def matcher = "${ParseCell(cell)}" =~ regexp       
        } 
    
    def addColumnData={Cell cell ->
        
        Sheet targetSheet = target.sheet
        Row row = targetSheet.getRow(cell.getRowIndex()+target.rowDiff)
        if (row==null){
            
            row=targetSheet.createRow(cell.getRowIndex()+target.rowDiff)
        }
        Cell targetCell=row.createCell(cell.getColumnIndex()+target.colDiff)
        targetCell.setCellValue(actionExtras.randColNumbers.get(cell.getRowIndex()))
        } 
        
    def doActionExtras(){
        
        println "actionExtras..."
        if (this.action.getAction()=="ADD_COLUMN_DATA" ){
            def randNumbers = []
            target.getSize().times { randNumbers << it+1 } 
            // this could be done better, i want to generate a randomly sorted set of data for the lane number
           
            randNumbers.sort{Math.random()}
            actionExtras.putAt("randColNumbers",randNumbers)
        }
    }
    
    def makeDemoHeaders(Sheet targetSheet){
        def firstCellNum = targetSheet.getRow(targetSheet.getFirstRowNum()).getFirstCellNum()
        targetSheet.shiftRows(targetSheet.getFirstRowNum(),targetSheet.getLastRowNum(),1)
        def headers=["Sample Name","Lane Number"]
       
        Row headerRow=targetSheet.createRow(0)
        CellStyle cstyle = targetSheet.getWorkbook().createCellStyle()
        Font bfont = targetSheet.getWorkbook().createFont()
        bfont.setBoldweight(Font.BOLDWEIGHT_BOLD)
        cstyle.setFont(bfont)
        
        headers.eachWithIndex{obj,i->
            Cell cell = headerRow.createCell(i+firstCellNum)
            cell.setCellStyle(cstyle)
            cell.setCellValue(obj)
        }
        return targetSheet
    }
    }
    
    
