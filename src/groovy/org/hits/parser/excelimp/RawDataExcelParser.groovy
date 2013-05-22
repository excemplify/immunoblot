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


/**
 *
 * @author jongle
 */
class RawDataExcelParser implements Parser{
    
    Source source   
    Target target
    Action action
    String name

    def blankcells
    def origWorkbook
    def dataName
    def blotNum
    def targetSheetPrefix
   
    //what data do we need to come from the knowledge base from the templates when we define the source
    // some comes from the setup template
    // others comes from the loading
    //
    def knowledgeMap=[:]
    
    final static actionsAvailable=[ImmunoParserAction.COPY,ImmunoParserAction.MATCH_AND_SPLIT]
    
    def configure(StateAndQueue state, Map configurations) throws ParserConfigException{
        // configurations has: target, action, name, nodeType, sources
        //first check if we need one source or multiple... assuming use of expanding excelsource in both
        if (!state.state.workbook){
            origWorkbook=WorkbookFactory.create(state.state.file) 
            //origWorkbook=WorkbookFactory.create(state.state.experimentWorkbookFile) 
            state.state.workbook=origWorkbook
        }
        else origWorkbook=state.state.workbook
     
        log.info state.state.fileName
        /*convention: the raw data file name follows the formate $Protein_..._blot$blotnum
         **/
        this.dataName=processFileName(state.state.fileName)           
        this.blotNum=dataName.tokenize("_").last()
        this.dataName=dataName.minus("_$blotNum")
        println "blotNum $blotNum"
        //this.targetSheetPrefix="Gel Inspector" //this could be configured by the webapp selecting the template
        this.action=ImmunoParserAction.valueOf(configurations.action)
        // we need to get the correct setupsheet for the corresponding blot number. So, we set the source
       
        if (configurations.sources.size()>1){
            println "setting sources"
  
            configurations.sources.each{ //this doesn't seem to actually work at the moment, can't figure out why but we get around it
                if (it.isDirty('sheetName')){
                    println "dirty"
                    it.sheetName=it.sheetName.getPersistentValue()
                }
                println "source sheet name ${it.sheetName}"
                if (it.sheetName.contains("auto_Loading")){ //it seems to be caching or saving the domain object somewhere
                    println "changing to add blot Num"
                    it.sheetName = "auto_Loading_$blotNum"// for when we have multiple blots we need to make sure we are getting the corresponding lane loading
                }
                
            }
            println "and after making sure domain object is the same..."
            configurations.sources.each{
                println "source sheet name ${it.sheetName}"
                
            }
            source= new MultiSource(configurations.sources, state)
            if (action==ImmunoParserAction.OUTERPRODUCT_COLUMNS_MATCH || action==ImmunoParserAction.COPY || action==ImmunoParserAction.MATCH_AND_SPLIT)
            {
                source.setSourceType(SourceType.OUTERPRODUCT)
            }
           
        
            if (action==ImmunoParserAction.COPY){
                source.setSourcesSameLength()
            } 
        }
        else source = new ExpandingExcelSource(configurations.sources.first(), state)
        // set which action
        knowledgeMap=source.getKnowledgeMap()
        println action
        //set the target
        this.target=new TemplateExcelTarget(configurations.target,state)
        this.targetSheetPrefix=configurations.target.sheetName
        if (state.state.experimentWorkbook.getSheet("$targetSheetPrefix $blotNum")){ //we want to test if we have already written to
            target.sheet=state.state.experimentWorkbook.getSheet("$targetSheetPrefix $blotNum")
        }
        else {
            def newsheet=state.state.experimentWorkbook.createSheet("$targetSheetPrefix $blotNum")
            copyRows(target.sheet,newsheet) //we do this here to make sure we are just copying the template part, not overwriting data
            target.sheet=newsheet
        }
        if (action==ImmunoParserAction.COPY) target.findLastColumnAndReset(0)//headers are in the first row
     
        target.setRowDiff(source.getFirstRow())
        target.setColDiff(source.getFirstColumn())
        
    }
   
    
    StateAndQueue parse(StateAndQueue state) throws ParsingException{
        // how parsing this spreadsheet is different:
        // we get the name (of the protein?) and blot number from the file name if no blot number specified assume sheet name "Gel Inspector Blot 1
        // we need to skip rows that are blank 
        // to generate the cells/time/index data we need to get the unjumbled bits from the setup sheet again and match them
        //  with the lane number(index) that is in the setup sheet. So, two different sheet sources needed here
        // we need to get the lane number to determine which row a piece of data comes from. Do we want the data ordered by time?
    
        
        println "parsing spreadsheet now...${name}"
       
        //sheet=source.traverse(doAction)
        source.traverse(doAction)
              
        
        state.state.blankcells=blankcells
        byte[] parsedBook
        def outstream = new ByteArrayOutputStream()
        
        state.state.workbook=origWorkbook //to make sure we don't overwrite old changes //but now there's a problem with the sheets
        //origWorkbook.write(outstream)
        state.state.experimentWorkbook.write(outstream)
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

     if(cell){
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
           
        case    ImmunoParserAction.OUTERPRODUCT_COLUMNS:
            outerproduct(cells)
            break
        case ImmunoParserAction.MATCH_AND_SPLIT:
            print "matchandsplitaction"
            matchAndSplit(cells)
            break
        case ImmunoParserAction.COPY:
            matchLaneAndCopy(cells)
            break
                 
        }
    }
    
   
    def matchLaneAndCopy={List cellLists ->
        //we assume we have done the 
        println "copy $cellLists"
        def bandNumList=cellLists[2]  //we know the position of each of the cellLists from the ordering of sources in the bootstrap.
        // otherwise it would be good to make use of the "knowledge" in Lei's templates.
        
        def bandSet= new HashSet()
        bandNumList.each{
            if (it!=null){
                bandSet.add(parseCell(it) as int)
            }
        }
        
        Row headerRow = target.sheet.getRow(target.firstRow-1)
        
        
        bandSet.eachWithIndex{it,n -> 
            // first check for dupes
            
            def dupIt=0
            def headerIt = headerRow.cellIterator()
            while (headerIt.hasNext()){
                Cell currCell = headerIt.next()
                println currCell.getStringCellValue()
                
                if (currCell.getStringCellValue()=="$dataName-${it}".toString()) {
                    println "duplicate!"
                    dupIt++
                }
            }
            
            Cell headerCell = headerRow.getCell(target.firstColumn+n)?:headerRow.createCell(target.firstColumn+n)
            println "band $dupIt"
            def headerLabel= dupIt>0?"$dataName-${it}($dupIt)":"$dataName-${it}"
            headerCell.setCellValue(headerLabel)
        }
        def indexList=cellLists[0]
        def dataList=cellLists[1]
        
        
        dataList.eachWithIndex{dataCell,n->
                    println "dataCell $dataCell n $n"
            if (dataCell!=null && indexList[n]!=null && bandNumList[n]!=null){
                log.info "valid dataCell"
                def data = parseCell(dataCell)
                
                def laneNumber = parseCell(indexList[n]).tokenize().get(1) as int
                def colNumber = parseCell(bandNumList[n])-1 as int
                  println "laneNumber $laneNumber  colNumber $colNumber"
                Row row = target.sheet.getRow(target.firstRow-1+laneNumber)?:target.sheet.createRow(target.firstRow-1+laneNumber)
                Cell cell = row.getCell(target.firstColumn+colNumber)?:row.createCell(target.firstColumn+colNumber)
                cell.setCellValue(data)
            }
        }
        println "finish copy"
    }
    
    // new strategy get the lane and name from the loading sheet, 
    
    //    def matchAndSplit={List cellLists-> 
    //        //format for first cellLi
    //        println "cellLists $cellLists"
    //        def matchLists=cellLists[0]
    //        println "matchLists $matchLists"
    //        def times=[] 
    //        def doses=[] 
    //        def inhibitors=[]
    //        cellLists[2].each{times<<"${parseCell(it)}"}
    //        // print "times $times"
    //        cellLists[3].each{doses<<"${parseCell(it)}"}
    //        
    //        if(cellLists[4]){
    //            cellLists[4].each{inhibitors<<"${parseCell(it)}"}
    //        }else{
    //          inhibitors<<"n/a"
    //        }
    //        //cellLists[1].each{times<<"${parseCell(it)}"}
    //        
    //        def cellNames = []
    //        cellLists[1].each{cellNames<<parseCell(it)}
    //        // cellLists[2].each{cellNames<<parseCell(it)}
    //        println "cellNames $cellNames"
    //        def stimuli=["+","-"] // the two kinds of stimulus markers we have
    //        
    //        for (int i=0;i<matchLists.size();i=i+2){
    //            def thisRowCells=[]
    //           
    //            def laneNumber=parseCell(matchLists.get(i)) as int //lane number
    //            //  println "matchLists.get $i = ${matchLists.get(i)} -> $laneNumber"
    //            def cellLabel=parseCell(matchLists.get(i+1))   //cellLabel 60.0 primary mouse hepatocytes 1.0 inhibitor
    //            thisRowCells<< times.find{it=="${cellLabel.split(" ")[0]}"} //need that space so that we don't match all to 0.0
    //            
    //            thisRowCells<< doses.find{it=="${cellLabel.split(" ")[-2]}"}
    //            
    //            thisRowCells<< inhibitors.find{it=="${cellLabel.split(" ")[-1]}"}
    //            
    //            thisRowCells<< cellNames.findAll{cellLabel.contains(it)}.max{it.length()}             
    //            thisRowCells<< stimuli.find{ cellLabel.contains(" $it")}?:""
    //            // println "thisRowCells $thisRowCells"
    //            // now write each of them for each row
    //            Row row=target.sheet.getRow(target.firstRow-1+laneNumber)
    //            if(row==null){
    //                row=target.sheet.createRow(target.firstRow-1+laneNumber)
    //            }
    //            def indexCell=row.getCell(target.firstColumn)?:row.createCell(target.firstColumn) //set the lane numbers
    //            indexCell.setCellValue("$laneNumber")
    //            thisRowCells.eachWithIndex{cellValue,m->
    //                Cell targetCell = row.getCell(target.firstColumn+1+m)?:row.createCell(target.firstColumn+1+m)
    //                targetCell.setCellValue(cellValue)
    //                  
    //            }
    //     
    //        }
    //         
    //          
    //    }
       
    def matchAndSplit={List cellLists-> 
        //format for first cellLi
        println "rawdata parser cellLists $cellLists"
        def matchLists=cellLists[0]
        println "rawdata parser matchLists $matchLists"
        def times=[] 
        def doses=[] 
        def inhibitors=[]
        cellLists[2].each{times<<"${parseCell(it)}"}
        // print "times $times"
        cellLists[3].each{doses<<"${parseCell(it)}"}
        
        if(cellLists[4]){
            cellLists[4].each{inhibitors<<"${parseCell(it)}"}
        }else{
            inhibitors<<"n/a"
        }
        //cellLists[1].each{times<<"${parseCell(it)}"}
        
        def cellNames = []
        cellLists[1].each{cellNames<<parseCell(it)}
        // cellLists[2].each{cellNames<<parseCell(it)}
        println "cellNames $cellNames"
        def stimuli=["+","-"] // the two kinds of stimulus markers we have
        int stimulipos=0
        for (int i=0;i<matchLists.size();i=i+2){
            def thisRowCells=[]
           
            def laneNumber=parseCell(matchLists.get(i)) as int //lane number
            //  println "matchLists.get $i = ${matchLists.get(i)} -> $laneNumber"
            def cellLabel=parseCell(matchLists.get(i+1))   //cellLabel 60.0 primary mouse hepatocytes 1.0 inhibitor
            thisRowCells<< times.find{it=="${cellLabel.split(" ")[0]}"} //need that space so that we don't match all to 0.0
            if(cellLabel.split(" ")[-1]=="+" ||cellLabel.split(" ")[-1]=="-"){
                stimulipos=1
            }
            if(!cellLists[4] & cellLabel.split(" ")[-1-stimulipos]!="n/a"){  //only for dirty experiments
                
                thisRowCells<< doses.find{it=="${cellLabel.split(" ")[-1-stimulipos]}"}
            
                thisRowCells<< "n/a"
            
            }else{
               
            
                thisRowCells<< doses.find{it=="${cellLabel.split(" ")[-2-stimulipos]}"}
            
                thisRowCells<< inhibitors.find{it=="${cellLabel.split(" ")[-1-stimulipos]}"}
            }
            
            thisRowCells<< cellNames.findAll{cellLabel.contains(it)}.max{it.length()}             
            thisRowCells<< stimuli.find{ cellLabel.contains(" $it")}?:""
            // println "thisRowCells $thisRowCells"
            // now write each of them for each row
            Row row=target.sheet.getRow(target.firstRow-1+laneNumber)
            if(row==null){
                row=target.sheet.createRow(target.firstRow-1+laneNumber)
            }
            def indexCell=row.getCell(target.firstColumn)?:row.createCell(target.firstColumn) //set the lane numbers
            indexCell.setCellValue("$laneNumber")
            thisRowCells.eachWithIndex{cellValue,m->
                Cell targetCell = row.getCell(target.firstColumn+1+m)?:row.createCell(target.firstColumn+1+m)
                targetCell.setCellValue(cellValue)
                  
            }
     
        }
         
          
    }
    
    def outerProductMatch={List cellLists ->  //flattens to one column //we use this closure to set up the cells/time/lanenumber/stimulation columns in the sheet, problem is with stimulation
        //Sheet targetSheet=target.sheet
        //first two lists  
        def matchLists=cellLists[0]
        def nameToLaneMap=[:]
        for (int i=0;i<matchLists.size();i=i+2){
            def name=parseCell(matchLists.get(i+1))
            if(parseCell(matchLists.get(i)))
            nameToLaneMap.putAt((name),parseCell(matchLists.get(i)))
            else
            nameToLaneMap.putAt((name), "n/a")
        }
          
        println "rawdata outerProduct match" nameToLaneMap
        //all remaining lists we do the combos stuff with
        def allcombos=cellLists[1..-1].combinations()
         
        println "rawdata outerProduct match allcombos $allcombos"
          
        allcombos.eachWithIndex{it,n->
             
              
            String targetCellName=""    
             
            it.each{cell->        
                if (targetCellName=="") targetCellName=parseCell(cell)
                else
                targetCellName="${parseCell(cell)} ${targetCellName}" //whether we prepend or append depends on ordering of sources
                   
            }
            println "targetCellName $targetCellName"
             
            def laneNumber = nameToLaneMap.get(targetCellName) as int
            
            Row row=target.sheet.getRow(target.firstRow-1+laneNumber)
            if(row==null){
                row=target.sheet.createRow(target.firstRow-1+laneNumber)
            }
            def indexCell=row.getCell(target.firstColumn)?:row.createCell(target.firstColumn) //set the lane numbers
            indexCell.setCellValue(nameToLaneMap.get(targetCellName))
            it.eachWithIndex{cell,m->
                Cell targetCell = row.getCell(target.firstColumn+1+m)?:row.createCell(target.firstColumn+1+m)
                targetCell.setCellValue(parseCell(cell))
                targetCell.setCellType(cell.getCellType())
            }
        }
                                
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
                    toCell.setCellType(oldCell.getCellType())
                    //   toCell.setCellStyle(oldCell.getCellStyle())
                }
            }
          
          
        }
        return toSheet
    }  
  
    def processFileName(String fileName){
        println fileName.tokenize(".")
        return fileName.tokenize(".").first()
    }
}

