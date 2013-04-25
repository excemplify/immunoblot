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
 * This source will be configured by giving a starting cell, we will assume the sourceString describes a single cell eg "a4"
 *  This cell is assumed to be the leftmost cell in the row of headers for the parser. Each column must have a header
 *    q? merged cells in for headlines -> give all the same label but with an iteration number
 *    q? need to set some kind of constraint for checking the rowEnd criteria -> must be in the config map, a closure
 *    q? assume that first column will define the number of rows we need (possibly extend to allow for selecting a column)
 */

package org.hits.parser.excelimp

import org.hits.parser.core.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Workbook
import org.hits.parser.extractExcel.PatchedPoi
import org.hits.parser.SourceDef
import org.hits.ui.Knowledge

/**
 *
 * @author jongle
 */
class ExpandingExcelSource implements Source{
    
    String sourceString
    CellRangeAddress sourceCRA
    Sheet sheet
    int firstRow,lastRow,firstColumn,lastColumn
    int size
    int currRow,currCol //holds the row and index of the last cell we retrieved
    def knowledgeList=[]
    
    //we assume now we are only detecting the end of the column, not the number of columns
    // for non contiguous columns we should have more than one source
    public ExpandingExcelSource(SourceDef sourceDef, StateAndQueue state){
        //this.sheet = state.state.sheet //we always get the sheet from the state?
       

        println "sheetNum: ${sourceDef.sheetNum}"
        println "sheetNum: ${sourceDef.sheetName}"
        
        this.sheet=state.state.workbook.getSheet(sourceDef.sheetName)  //this could be problematic for sources requiring a different workbook to the one in the state
        //PatchedPoi.getInstance().clearValidationData(this.sheet)
        if (sheet==null) {
            
            this.sheet=state.state.workbook.getSheetAt(sourceDef.sheetNum)
            if (sourceDef.sheetName!=""){
                state.state.workbook.setSheetName(sourceDef.sheetNum,sourceDef.sheetName)
            }
        }
        println sheet.getSheetName()
        this.sourceCRA = CellRangeAddress.valueOf(sourceDef.cellRange)
        this.firstRow=sourceCRA.getFirstRow()
        this.firstColumn=sourceCRA.getFirstColumn()
        this.lastColumn=sourceCRA.getLastColumn()
        this.lastRow=sourceCRA.getLastRow()
        
        setKnowledgeList(sourceDef.template.knowledgeList as List)
       

        if (lastRow==firstRow){
            this.lastRow = findColumnEnd(firstColumn,endOfColumnTest) //assuming we will count the first row
            println "last Row ${lastRow}"
         //reset cra
             sourceCRA.setLastRow(lastRow)
        }
        this.size= sourceCRA.getNumberOfCells()
        this.sourceString=sourceCRA.formatAsString()
        println "${sourceString} source size ${this.size}"
        resetCellCounter()
       
    }
    
    public ExpandingExcelSource(SourceDef sourceDef, Workbook workbook, StateAndQueue state){
        println sourceDef.sheetNum
        
            this.sheet=workbook.getSheet(sourceDef.sheetName) //if the number doesn't work try the name careful here, not the same for all parse
        if (sheet==null) {
            
            this.sheet=state.state.workbook.getSheetAt(sourceDef.sheetNum)
            if (sourceDef.sheetName!=""){
                state.state.workbook.setSheetName(sourceDef.sheetNum,sourceDef.sheetName)
            }
        }
        
        println sheet.getSheetName()
        this.sourceCRA = CellRangeAddress.valueOf(sourceDef.cellRange)   
        this.firstRow=sourceCRA.getFirstRow()
        this.firstColumn=sourceCRA.getFirstColumn()
        this.lastColumn=sourceCRA.getLastColumn()
        this.lastRow=sourceCRA.getLastRow()
        
        setKnowledgeList(sourceDef.template.knowledgeList as List)
        println "associated knowledge $knowledgeList"
        
        if (lastRow==firstRow){
            this.lastRow = findColumnEnd(firstColumn,endOfColumnTest) //assuming we will count the first row
            println "last Row ${lastRow}"
         //reset cra
             sourceCRA.setLastRow(lastRow)
        }
        this.size= sourceCRA.getNumberOfCells()
        this.sourceString=sourceCRA.formatAsString()
        println "${sourceString} source size ${this.size}"
        resetCellCounter()
       
    }
    
   
    
    def setSourceCRA(String sourceString){
        sourceCRA=CellRangeAddress.valueOf(sourceString)
    }
    
    def getSourceCRA(){
        return sourceCRA
    }
    
    def resetSize(Map configs){
        
       println "resetting source"
       if (configs.firstColumn) {
           this.sourceCRA.setFirstColumn(configs.firstColumn)
           this.firstColumn=configs.firstColumn
       }
       if (configs.lastColumn) {
           this.sourceCRA.setLastColumn(configs.lastColumn)
           this.lastColumn=configs.lastColumn
       }
       if (configs.firstRow) {
           this.sourceCRA.setFirstRow(configs.firstRow)
           this.firstRow=configs.firstRow
       }
       if (configs.lastRow) {
           this.sourceCRA.setLastRow(configs.lastRow)
           this.lastRow=configs.lastRow
       }
       this.sourceString=sourceCRA.formatAsString()
       this.size=sourceCRA.getNumberOfCells()
      
        
       
   
    }
    
    
    def findColumnEnd(int colnumber,endOfColumnTest){
        int i=firstRow
        println "first row $firstRow"
        println "finding column end ${sheet.getLastRowNum()}"
        Cell cell = sheet.getRow(firstRow).getCell(colnumber) 
            def origCellType = cell.getCellType()
        while (i<=sheet.getLastRowNum() ){
          
            cell = sheet.getRow(i).getCell(colnumber) //want the first data row not the header          
            if (endOfColumnTest(cell,origCellType)) {
                i++
            }
            else break
        
        }
        return i-1
    }
    
    def resetCellCounter(){
        currCol=firstColumn
        currRow=firstRow
    }
    
    def getNextCell(){
        //assuming a left to right then down direction
       
        if (currRow>lastRow) return null
        else{
            Cell cell= sheet.getRow(currRow).getCell(currCol)
            if (cell==null) return null
            else{
                if (currCol>=lastColumn ){
                    currCol=firstColumn
                    currRow=currRow+1
                }
                else {
                    
                        currCol=currCol+1
                }
            return cell    
            }
        
            
        }
    }
    
    
    def traverse(action){
        
        println action
        for (int i=sourceCRA.getFirstRow(); i<=sourceCRA.getLastRow();i++){
                Row row = sheet.getRow(i)
                for (int j=sourceCRA.getFirstColumn(); j<=sourceCRA.getLastColumn();j++){
                    println "coords ${i},${j}"
                    Cell cell = row.getCell(j)
                    currRow=i
                    currCol=j
                    action.call(cell)
                    
                }
        }
       return sheet
                        
    }
    
    def traverseRowise(action){
        
        println "traversing rowise..."
        println action
        def cells=[]
        for (int i=sourceCRA.getFirstRow(); i<=sourceCRA.getLastRow();i++){
                Row row = sheet.getRow(i)
                cells =[]
                for (int j=sourceCRA.getFirstColumn(); j<=sourceCRA.getLastColumn();j++){
                    println "coords ${i},${j}"
                    Cell cell = row.getCell(j)
                    cells<<cell
                    
                }
                action.call(cells)
        }
       return sheet
                        
    }
    
  def getFirstRow(){
      return sourceCRA.getFirstRow()
  } 
  
 def getFirstColumn(){
     return sourceCRA.getFirstColumn()
 }   
 
  def getSize(){
      return this.size
  }
  
  def endOfColumnTest ={Cell cell,origCellType ->
      
        return (cell!=null && cell.getCellType()==origCellType) 
      
  }
  
  def setKnowledgeList(knowledge){
      //iterate through columns to find out what we have in the list that is applicable for this source
      for (int i=firstColumn;i<=lastColumn;i++){
          def columnLabel=knowledge.find{it.firstCol=="$i"}?.knowledgeName
          println columnLabel
         if (columnLabel!=null){
             this.knowledgeList<<columnLabel
         }
      }
  }  
    
    
  def getKnowledge(){
      return this.knowledgeList
  }  
}

