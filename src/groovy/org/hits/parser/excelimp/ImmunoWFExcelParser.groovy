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
import org.apache.poi.ss.usermodel.*
import org.hits.ui.exceptions.*


/**
 *
 * @author jongle
 */
class ImmunoWFExcelParser implements Parser{
    
    Source source   // TwoExcelSource
    Target target
    Action action
    String name

    def blankcells
    def origWorkbook
    
  
    
   def configure(StateAndQueue state, Map configurations) throws ParserConfigException{
      // configurations has: target, action, name, nodeType, sources
      //first check if we need one source or multiple... assuming use of expanding excelsource in both
      if (!state.state.workbook){
        origWorkbook=WorkbookFactory.create(state.state.file)
        state.state.workbook=origWorkbook
      }
      else origWorkbook=state.state.workbook
      this.action=ImmunoParserAction.valueOf(configurations.action)
      if (configurations.sources.size()>1){
          println "setting sources"
          source= new MultiSource(configurations.sources, state)
         
          println configurations.action
          if (action==ImmunoParserAction.OUTERPRODUCT_COLUMNS || action==ImmunoParserAction.OUTERPRODUCT_COLUMNS_RANDOMIZE) source.setSourceType(SourceType.OUTERPRODUCT)
          else source.setSourceType(SourceType.CONCAT)
      }
      else source = new ExpandingExcelSource(configurations.sources.first(), state)
       // set which action
      this.action=ImmunoParserAction.valueOf(configurations.action)
      println action
      //set the target
      this.target=new TemplateExcelTarget(configurations.target,state)
      target.setRowDiff(source.getFirstRow())
      target.setColDiff(source.getFirstColumn())
   }
   
    
   StateAndQueue parse(StateAndQueue state) throws ParsingException{
        println "parsing spreadsheet now...${name}"
       
        //sheet=source.traverse(doAction)
        source.traverse(doAction)
              
        
        state.state.blankcells=blankcells
        byte[] parsedBook
        def outstream = new ByteArrayOutputStream()
        //target.template.write(outstream)
        
        def targetSheetName = target.sheet.getSheetName()
        def targetSheet=origWorkbook.getSheet(targetSheetName)
        if (targetSheet==null)
            targetSheet=origWorkbook.createSheet(targetSheetName)
        
        targetSheet=copyRows(target.sheet,targetSheet) //actually copies cell by cell, can't just copy the whole sheet
        // it might be easier to copy the cells from the blank template to a new sheet from the original workbook
        //set that as the target sheet
        state.state.workbook=origWorkbook //to make sure we don't overwrite old changes //but now there's a problem with the sheets
        origWorkbook.write(outstream)
        parsedBook=outstream.toByteArray()
        state.state.parsedFile=parsedBook
        state.state.success=true
        return state
        
        
          
   }
    
  def setSource(Source source){
      this.source=source
  }  
  
  def setTarget(Target target){
        this.target=target
    }
    
  def setAction(Action action){
        this.action=action
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
           // defaultErrorHandler(cell)
            //cell.setCellStyle(colourCell)
            return ""
           
        }
    }
    
    def doAction={cells->
        
        switch (action) {
            case ImmunoParserAction.TRANSPOSE :
            transpose(cells)
            break
            case   ImmunoParserAction.COPY:
            copy(cells)
            break
            case    ImmunoParserAction.OUTERPRODUCT_COLUMNS:
            outerproduct(cells)
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
            case ImmunoParserAction.OUTERPRODUCT_COLUMNS_RANDOMIZE:
            outerProductRandomize(cells)
            break
                 
        }
    }
    
    def transpose={Cell cell ->
        Sheet targetSheet = target.sheet
        int transrownum=cell.getColumnIndex()
        int transcolnum=cell.getRowIndex()
       
        Row row = targetSheet.getRow(transrownum+target.firstRow-source.firstColumn)
        if (row==null){
            
            row=targetSheet.createRow(transrownum+target.firstRow-source.firstColumn)
        }
        Cell targetCell=row.createCell(cell.getRowIndex()+target.firstColumn-source.firstRow)
        targetCell.setCellValue(parseCell(cell))
    }
    
    def copy={Cell cell ->
        Sheet targetSheet = target.sheet
       
        Row row = targetSheet.getRow(cell.getRowIndex()+target.rowDiff)
        if (row==null){
            
            row=targetSheet.createRow(cell.getRowIndex()+target.rowDiff)
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
        cells.each{ targetCellVal="${targetCellVal} ${parseCell(it)}"}
        println targetCellVal
        targetCell.setCellValue(targetCellVal)
    
    }
    
    def outerproduct={List cellLists ->  //flattens to one column
          //Sheet targetSheet=target.sheet
          def allcombos=cellLists.combinations()
          println "targetlist size ${allcombos.size()}"
          
          allcombos.eachWithIndex{it,n->
              
              Row row=target.sheet.getRow(target.firstRow+n)
                  if(row==null){
                      row=target.sheet.createRow(target.firstRow+n)
                  }
                  
                Cell targetCell=row.getCell(target.firstColumn)?:row.createCell(target.firstColumn)
                println "target cords ${targetCell.getRowIndex()} ${targetCell.getColumnIndex()}"
                String targetCellValue=""
                it.each{
                    targetCellValue="${targetCellValue} ${parseCell(it)}"
                }
                println targetCellValue
                targetCell.setCellType(Cell.CELL_TYPE_STRING)
                targetCell.setCellValue(targetCellValue)
                
          }
        
          
      }

def outerProductRandomize={List cellLists ->  //flattens to one column
          //Sheet targetSheet=target.sheet
          def allcombos=cellLists.combinations()
          println "targetlist size ${allcombos.size()}"
          println allcombos
          def numlanes=allcombos.size()
          def laneNumbers =[]
          (1..numlanes).each{laneNumbers.add(it)}
          println laneNumbers
          Collections.shuffle(laneNumbers) //replace this with the correct shuffling routine
          allcombos.eachWithIndex{it,n->
              
              Row row=target.sheet.getRow(target.firstRow+laneNumbers[n]-1) //no zero lane Number so have to -1 
                  if(row==null){
                      row=target.sheet.createRow(target.firstRow+laneNumbers[n]-1)
                  }
                  
                Cell targetCell=row.getCell(target.firstColumn)?:row.createCell(target.firstColumn)
                println "target cords ${targetCell.getRowIndex()} ${targetCell.getColumnIndex()}"
                String targetCellValue=""
                it.each{
                    targetCellValue="${targetCellValue} ${parseCell(it)}"
                }
                println targetCellValue
                targetCell.setCellType(Cell.CELL_TYPE_STRING)
                targetCell.setCellValue(targetCellValue)
                
          }
        
          
      }

    def split={Cell cell, String regexp->
        Sheet targetSheet = target.sheet
        
        Row row = targetSheet.getRow(cells.first().getRowIndex()+target.rowDiff)
        if (row==null){
            
            row=targetSheet.createRow(cell.first().getRowIndex()+target.rowDiff)
        }
        def matcher = "${ParseCell(cell)}" =~ regexp       
    }
    
   def defaultErrorHandler={Cell cell->
        Workbook wbook = cell.getSheet().getWorkbook()
        CellStyle colourCell = wbook.createCellStyle()
        colourCell.setFillForegroundColor(HSSFColor.AQUA.index)
        colourCell.setFillPattern(CellStyle.SOLID_FOREGROUND);
        
        cell.setCellStyle(colourCell)
        
    } 
    
  def copyRows(Sheet origSheet, Sheet toSheet){
      def iterator = origSheet.rowIterator()
      while (iterator.hasNext()){
          Row row = iterator.next()
          Row toRow=toSheet.getRow(row.getRowNum())?:toSheet.createRow(row.getRowNum())
          def cellIt=row.cellIterator()
          while (cellIt.hasNext()){
              Cell oldCell= cellIt.next()
              if (!toRow.getCell(oldCell.getColumnIndex())) {
                Cell toCell = toRow.createCell(oldCell.getColumnIndex())
                toCell.setCellValue(parseCell(oldCell))
              }
          }
          
          
      }
      return toSheet
  }  
}

