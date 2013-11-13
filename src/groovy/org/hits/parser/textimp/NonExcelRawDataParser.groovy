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

package org.hits.parser.textimp

import org.hits.parser.core.*
import org.hits.parser.excelimp.*
import org.hits.ui.exceptions.*
import org.apache.poi.ss.usermodel.*


/**
 *
 * @author jongle
 * 
 * This parser is used to take care of the raw data files that come out of the old equipment which are just text based.
 * The strategy is completely different to the excel based ones
 */
class NonExcelRawDataParser implements Parser{
	
    
    Source source   
    Target target
    Action action
    def dataName
    def blotNum
    String name
    def volumeRegex
    
    public configure(StateAndQueue state, Map configurations) throws ParserConfigException{
      
       // if (!state.state.workbook){
     //   origWorkbook=WorkbookFactory.create(state.state.file) 
     //   state.state.workbook=origWorkbook
      //}
     // else origWorkbook=state.state.workbook
     
      println state.state.fileName
      this.dataName=processFileName(state.state.fileName)
      this.blotNum=dataName.tokenize("_").last()
      this.dataName=dataName.minus("_$blotNum")
        
        source = new TextFileSource(configurations.sources, state)
        this.target=new TemplateExcelTarget(configurations.target,state)
        this.action=ImmunoParserAction.valueOf(configurations.action)
        
        
        
    }
    
    public StateAndQueue parse(StateAndQueue state) throws ParsingException{ 
    
        def sourceMap = source.getSourceLines()
        doAction(sourceMap)
        state.state.success=true
        byte[] parsedBook
        def outstream = new ByteArrayOutputStream()
        
        target.template.write(outstream)
        parsedBook=outstream.toByteArray()
        state.state.parsedFile=parsedBook
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
   
    def doAction={sourceLists->
        
        switch (action) {
           
            case    ImmunoParserAction.COPY:
            matchLaneAndCopy(sourceLists)
            break
            
                 
        }
    }
    
   
    
    //this is the closure from the excel based source file that we need to adapt 
     def matchLaneAndCopy={cellLists ->
        
        def indexList=cellLists.get("Lanes").first().toString().split().findAll { token-> token =~ /\d+?/}
        println indexList
       // def dataList=cellLists.get("Volume").each{ it.toString().split().findAll{token-> token =~ /\d+?\.\d+?e\d+?/} }
        def dataList=[:]
        cellLists.get("Volume").eachWithIndex{it,n->dataList.put((n),it.toString().split().findAll{token-> token =~ /(\d+?\.\d+?e\d+?)|\d+?/ })}
        println "indexList $indexList"
        
        println "dataList $dataList"
        
        //write the data to a template
        def sheet = target.sheet
        def firstRow=target.getFirstRow()
        def firstCol = target.getFirstColumn()
        def numBands=dataList.size()
        indexList.each{
            def index=it as int
            dataList.each{bandNum, data->
                Row currRow = sheet.getRow(firstRow+((int)index-1)*numBands+bandNum)?:sheet.createRow(firstRow+((int)index-1)*numBands+bandNum)
                def laneCell = currRow.getCell(firstCol)?:currRow.createCell(firstCol)
                laneCell.setCellValue("Lane $index")
                def bandCell = currRow.getCell(firstCol+1)?:currRow.createCell(firstCol+1)
                bandCell.setCellValue(bandNum+1)
                def volCell = currRow.getCell(firstCol+2)?:currRow.createCell(firstCol+2)
                volCell.setCellValue(data[(int)index])
            }
        }
        
        
    }
    
   def processFileName(String fileName){
        println fileName.tokenize(".")
        return fileName.tokenize(".").first()
    } 
}

