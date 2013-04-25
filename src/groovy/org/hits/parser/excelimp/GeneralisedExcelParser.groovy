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
import org.hits.parser.extractExcel.PatchedPoi

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Color
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.poifs.filesystem.POIFSFileSystem

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
/**
 *
 * @author jongle
 */
class GeneralisedExcelParser implements Parser{

    Source source
    Target target
    Action action
    String name
    
   // int sheetnum
     def blankcells
    Sheet sheet
    
    def parsingResults=[:]
    
    public StateAndQueue parse(StateAndQueue state) throws ParsingException{
        
        
        println "parsing spreadsheet now...${name}"
        sheet=source.traverse(doAction)
        println sheet
        //state.state.sheet=sheet
        sheet=state.state.targetsheet
        println sheet
        //procedure:
        //  do a cell walk to traverse the area specified in source placing parsing results in some kind of map
        //  perform action to these results
        //  write out results to target
        //  update state and queue
        // get the spreadsheet itself from the state
        
        // do cell walk on the CRA from the 
        // what the callback method onCell actually does is determined by action type
        // error is determine by the errorHandler
        // update state
        //state.state.parsedFilePath=newfilepath
           // File parsedFile = new File(newfilepath)
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
              // PatchedPoi.getInstance().clearValidationData(this.sheet)
            state.state.sheet=sheet
           
      //  }
      //  catch(IOException ex){
            
      //  }
      
        
         this.source=new ExcelSource(configurations.source, state)
        this.target = new ExcelTarget(configurations.target,state)
        target.setRowDiff(source.getFirstRow())
        target.setColDiff(source.getFirstColumn())
        this.action=ImmunoParserAction(configurations.action)
        
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
    
    def doAction={Cell cell->
        
        switch (action.getAction()) {
           case ImmunoParserAction.TRANSPOSE : 
                  transpose(cell)
                  break
           case   ImmunoParserAction.COPY:
                 copy(cell)   
                 
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
        
        Row row = sheet.getRow(cells.first().getRowIndex()+target.rowDiff)
        if (row==null){
            
            row=sheet.createRow(cell.first().getRowIndex()+target.rowDiff)
        }
        Cell targetCell=row.createCell(cell.first().getColumnIndex()+target.colDiff)
        String targetCellVal
        cells.each{ targetCellVal="${targetCellVal}${parseCell(it)}"}
        targetCell.setCellValue(targetCellVal)
    
    }
    
    def split={Cell cell, String regexp->
       Sheet targetSheet = target.sheet
        
        Row row = sheet.getRow(cells.first().getRowIndex()+target.rowDiff)
        if (row==null){
            
            row=sheet.createRow(cell.first().getRowIndex()+target.rowDiff)
        }
        def matcher = "${ParseCell(cell)}" =~ regexp       
        } 
    
    
    }
    
