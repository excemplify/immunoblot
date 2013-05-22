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
import org.apache.poi.hssf.usermodel.HSSFCell
import org.hits.ui.Randomization
import org.hits.ui.exceptions.RuleViolateException

/**
 *
 * @author jongle
 */
class SetupExcelParser implements Parser{
	
    Source source   
    Target target
    Action action
    String name

    def blankcells
    def origWorkbook

    Boolean randomization=true
    Boolean stimulus=false
    int min1=2
    int min2=2
    int min3=2
    def blotNum
 
    def knowledgeMap=[:]
    def requiredCellListOrdering=["TimePoints","Cells","Doses", "Inhibitors"] // this is the order we need the lists that we get from traversing the cells to come in
    def defaultMaximumLanes=18
    
    final static actionsAvailable=[ImmunoParserAction.OUTERPRODUCT_COLUMNS_RANDOMIZE]
    
    def configure(StateAndQueue state, Map configurations) throws ParserConfigException{
        // configurations has: target, action, name, nodeType, sources
        //first check if we need one source or multiple... assuming use of expanding excelsource in both
        if (!state.state.workbook){
            origWorkbook=WorkbookFactory.create(state.state.file)
            
            state.state.workbook=origWorkbook
        }
        else origWorkbook=state.state.workbook
        this.action=ImmunoParserAction.valueOf(configurations.action)
     
        blotNum = configurations.blotNum
        println "blotNum $blotNum"
        println "configs in parser config $configurations"
        if (configurations.randomization) {
            randomization=Boolean.valueOf(configurations.randomization)
            println "randomization? $randomization"
            if(configurations.min1){
                min1=Integer.valueOf(configurations.min1)
                println "min1? $min1"
            }  
            if(configurations.min2){
                min2=Integer.valueOf(configurations.min2)
                println "min2? $min2"
            }
            if(configurations.min3){
                min3=Integer.valueOf(configurations.min3)
                println "min3? $min3"
            }
        }
      
        if (configurations.stimulus) {
            stimulus=Boolean.valueOf(configurations.stimulus)
            println "stimulus? $stimulus"
        }
      
      
        if (configurations.sources.size()>1){
            println "setting sources"
            source= new MultiSource(configurations.sources, state)
         
            println configurations.action
            if (action==ImmunoParserAction.OUTERPRODUCT_COLUMNS || action==ImmunoParserAction.OUTERPRODUCT_COLUMNS_RANDOMIZE) source.setSourceType(SourceType.OUTERPRODUCT)
            else source.setSourceType(SourceType.CONCAT)
        }
        else source = new ExpandingExcelSource(configurations.sources.first(), state)
        // set out a list of stuff we need to know about what is in what column
        knowledgeMap=source.getKnowledgeMap() //now the numbering of the sources corresponds to the ordering of the cell list
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
        println "targetSheetName = $targetSheetName"
        targetSheetName=targetSheetName+"_blot$blotNum"
        //targetSheetName=target.sheet.getWorkbook().setSheetName(target.sheet.getWorkbook().getSheetIndex(targetSheetName), "${targetSheetName}_${blotNum}")
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
    
    def parseCell(cell){
    // println "here SetupExcelParser parseCell"
        if (cell.class=="String".class) return cell
        else{
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
    
    def outerProductRandomize={List cellLists ->  //flattens to one column
        //Sheet targetSheet=target.sheet
          
        //currently we assume the correct ordering of the cellLists, we should check that first
    
        cellLists=reorderLists(cellLists)
        if(cellLists.size()<requiredCellListOrdering.size())   // for the case there is no inhibitor
        cellLists << ["n/a"]
                println "celllists setup $cellLists"  
          
        if (stimulus==true) {
            cellLists<<["+","-"]
        }
        
        
        def allcombos=cellLists.combinations()
        if(allcombos.size()<=defaultMaximumLanes){
       // println "allcombos setup $allcombos"  
        def numlanes=allcombos.size()
        def laneNumbers =[]
        (1..numlanes).each{laneNumbers.add(it)}
          
        laneNumbers=doRandomization(laneNumbers)
          
        allcombos.eachWithIndex{it,n->
              
            Row row=target.sheet.getRow(target.firstRow+laneNumbers[n]-1) //no zero lane Number so have to -1 
            if(row==null){
                row=target.sheet.createRow(target.firstRow+laneNumbers[n]-1)
            }
                  
            Cell targetCell=row.getCell(target.firstColumn)?:row.createCell(target.firstColumn)
            Cell laneNumCell=row.getCell(target.firstColumn-1)?:row.createCell(target.firstColumn-1)
            laneNumCell.setCellValue(laneNumbers[n])
            println "target cords ${targetCell.getRowIndex()} ${targetCell.getColumnIndex()}"
            String targetCellValue=""
            it.each{
                if (targetCellValue=="") targetCellValue=parseCell(it)
                else
                targetCellValue="${targetCellValue} ${parseCell(it)}"
            }
            println targetCellValue
            targetCell.setCellType(Cell.CELL_TYPE_STRING)
            targetCell.setCellValue(targetCellValue)
                
        }
        }else{
              throw new RuleViolateException("maimum 20-2(free) lanes rule is violated", "please consider replanning your experiment and update the setup file");
        }
          
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
  
    def doRandomization(List laneNumbers){
        def lanes = laneNumbers
        def randomlanes;
        if (randomization==true){
            println "doing randomisation"
            randomlanes=Randomization.getRandomizedSequence(laneNumbers, min1,min2,min3)
            // Collections.shuffle(lanes) //do proper shuffling routine here
        }
        else println "not randomizing lanes" 
        
        return lanes
    }
    
    def reorderLists(List cellLists){
        
        def newCellLists=[]
        def newOrderMap=[:]
        knowledgeMap.each{k,v->
            
            if (v!=requiredCellListOrdering[k]){ //we need to reorder
                
                def index=requiredCellListOrdering.indexOf(v) //find the location of the matching string
                
                newOrderMap.put((index),cellLists[k])
            }
            else newOrderMap.put((k),cellLists[k])
        }
              
        newOrderMap.sort().each{k,v->
            newCellLists<<v
        }
         
        return newCellLists
        
    }
}

