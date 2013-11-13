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
import org.hits.parser.SourceDef

import org.apache.poi.ss.usermodel.*

/**
 *
 * @author jongle
 */
class MultiSource implements Source{
    List<Source> sources
    SourceType sourceType //this tells us how to handle the multiple sources 
    //          whether to merge them together or outerproduct etc
    int firstRow
    int firstColumn
    int lastRow
    int lastColumn
    
    def knowledgeMap=[:]
    
    public MultiSource(SourceDef sourceDef, StateAndQueue state){
        sources=[new ExpandingExcelSource(sourceDef,state)]
    }
       
    public MultiSource(List<SourceDef> sourceDefs, StateAndQueue state){
        sources=[]
        Workbook experimentWorkbook
        println "start new MultiSource"
        sourceDefs.eachWithIndex{it,n->
            def source
            // println it.class
            println it.properties
            if (it.sourceType==SourceType.CURRWORKBOOK.toString()){ //in this case we want the experiment workbook as the source, not the uploaded file
              
                if(experimentWorkbook==null){
                    def instream = new ByteArrayInputStream(state.state.experimentWorkbookFile)
                    experimentWorkbook=WorkbookFactory.create(instream)       
               
                }
                state.state.experimentWorkbook=experimentWorkbook
                source=new ExpandingExcelSource(it,experimentWorkbook,state)
            }
            else{ source = new ExpandingExcelSource(it,state)} //source spreadsheet is the uploaded file
            println "setting up multi sources"
            sources<<source
            if (source.getKnowledge().size()>0){
                knowledgeMap.put((n), source.getKnowledge().first()) //first because at the moment we only have one item per source
            }
            println "multisource knowledgeMap $knowledgeMap"
        }
        
        firstRow=sources.collect{ it.getFirstRow()}.min()
        
     
        firstColumn=sources.collect{it.getFirstColumn()}.min()

        
        lastRow=sources.collect{ it.getLastRow()}.max()
        
        lastColumn=sources.collect{ it.getLastColumn()}.max()
    }
    
    def setSourceType(SourceType type){
        sourceType=type
    }
    
    // sets all the sources in the list to the first cell
    def initializeSources(){
        sources.each{
            it.resetCellCounter()
        }
    }
    
    def traverse(action){ //how this is done depends on the action. actually quite fundamentally different ways of calling them
        println "multi source traversing..." 
        println sourceType
        if (sourceType==SourceType.CONCAT){
            //not all sources need to be of the same dimensions, but does 
            println "hello, traversing with the concat"
            def cells=[]                           //the best it can until it runs out of cells in all sources
            boolean allEmpty=false
            while (!allEmpty){
                cells=[]
                sources.each{
                    def nextCell=it.getNextCell()
                
                    if (nextCell!=null){
                   
                        cells<<nextCell
                    }
                }
                if (cells.size()==0) allEmpty=true
                else {
                    println cells
                    action.call(cells)
                }
            }
           
        }
        else if (sourceType==SourceType.OUTERPRODUCT){ //tricky to generalise to n sources.
        
            def cellLists=[]
            println "hello, traversing with the outerproduct action"
        
            sources.each{ source->
            
                def cellList=[]
                for (int i=source.firstRow; i<=source.lastRow;i++){
                
                    Row row = source.sheet.getRow(i)
                    if(row){
                        for (int j=source.firstColumn; j<=source.lastColumn;j++){

                            Cell cell = row.getCell(j)
                            if(cell&&cell.getCellType()!=Cell.CELL_TYPE_BLANK){
                                cellList.add(cell)    
                            }
                
            
               

                        }
                    }
                }
                cellLists<<cellList

            }
            println "finish sources cellLists $cellLists"   
           
            action.call(cellLists)
        }else if (sourceType==SourceType.OUTERPRODUCTALLOWNULL){ //tricky to generalise to n sources.
        
            def cellLists=[]
            println "hello, traversing with the outerproduct allow null action"
        
            sources.each{ source->
            
                def cellList=[]
                for (int i=source.firstRow; i<=source.lastRow;i++){
                
                    Row row = source.sheet.getRow(i)
                    if(row){
                        for (int j=source.firstColumn; j<=source.lastColumn;j++){

                            Cell cell = row.getCell(j)
                 
                            cellList.add(cell)    
            
                
            
               

                        }
                    }
                }
                cellLists<<cellList

            }
            println "finish sources cellLists $cellLists"   
             
            action.call(cellLists)
        }
        
        println "finish multi source traversing"
    }
    
    def getFirstRow(){
        return this.firstRow
    }
   
    def getFirstColumn(){
        return this.firstColumn
    }

    def setSourcesSameLength(){
        sources.each{
            if (it.lastRow<this.lastRow) {
                it.resetSize("lastRow":this.lastRow)
            }
            
        }
    }
    
}

enum SourceType{
    CONCAT("CONCAT"), OUTERPRODUCT("OUTERPRODUCT"),TRACE("TRACE"), CURRWORKBOOK("CURRWORKBOOK"), SIMPLE("SIMPLE"),OUTERPRODUCTALLOWNULL("OUTERPRODUCTALLOWNULL")
    
    private String type
    
    SourceType(String type){
        this.type=type
    }
    
    def getType(){
        return type
    }
}