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
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Cell
/**
 *
 * @author rongji
 *
 */
class TwoExcelSource implements Source{

    String sourceString  //startpoint
    String sourceString2// startpoint

    CellRangeAddress sourceCRA
    CellRangeAddress sourceCRA2
    Sheet sheet
    int firstRow,lastRow,firstColumn,lastColumn
    int firstRow2,lastRow2,firstColumn2,lastColumn2
    int size, size2


    public TwoExcelSource(String source, StateAndQueue state){
        if (source.split(";").size()==2){
            sourceString=source.split(";")[0]
            sourceString2=source.split(";")[1]
        }else{
            sourceString=source
        }
        this.sourceCRA=CellRangeAddress.valueOf(sourceString)    //1x1 cell
        this.sourceCRA2=CellRangeAddress.valueOf(sourceString2)  //1x1 cell
        println sourceCRA
        //log.debug "something"
        println sourceCRA2
        println sourceCRA.getFirstRow()
        this.firstRow=sourceCRA.getFirstRow()
        this.firstColumn=sourceCRA.getFirstColumn()
        this.firstRow2=sourceCRA2.getFirstRow()
        this.firstColumn2=sourceCRA2.getFirstColumn()

        // this.lastRow=sourceCRA.getLastRow()
        // this.lastColumn=sourceCRA.getLastColumn()
        this.sourceString=sourceString
        this.sourceString2=sourceString2

        this.sheet = state.state.sheet

        //   Row firstRow2 = sheet.getRow(firstRow)
        println "top row ${sheet.getTopRow()}"
        if (state.state.params.numColumns){
            if(state.state.params.numColumns.toString().split(";").size()==2){
                int numColumns=state.state.params.numColumns.toString().split(";")[0] as int
                int numColumns2 = state.state.params.numColumns.toString().split(";")[1] as int
                println state.state.params.numColumns
                this.lastColumn=sourceCRA.getLastColumn()+numColumns
                this.lastColumn2=sourceCRA2.getLastColumn()+numColumns2


            }else{
                int numColumns = state.state.params.numColumns as int
                println state.state.params.numColumns
                this.lastColumn=sourceCRA.getLastColumn()+numColumns
            }
           
            
        }
        else{
            this.lastColumn = sheet.getRow(firstRow).getLastCellNum()-1
            this.lastColumn2 = sheet.getRow(firstRow2).getLastCellNum()-1
        }

       
        println "first column1 ${firstColumn} first column2 $firstColumn2"
        println "last column1 ${lastColumn} last column2 $lastColumn2"
        this.lastRow = findColumnEnd(firstRow, firstColumn, endOfColumnTest) //assuming we will count the first row
        this.lastRow2 = findColumnEnd(firstRow2, firstColumn2, endOfColumnTest) //assuming we will count the first row


        println "last Row ${lastRow} lasRow2 $lastRow2"
        //reset cra
        sourceCRA.setLastRow(lastRow-1)
        sourceCRA.setLastColumn(lastColumn)
        sourceCRA2.setLastRow(lastRow2-1)
        sourceCRA2.setLastColumn(lastColumn2)

        this.size= sourceCRA.getNumberOfCells()
        this.size2= sourceCRA2.getNumberOfCells()

        this.sourceString=sourceCRA.formatAsString()
        this.sourceString2=sourceCRA2.formatAsString()

        println "${sourceString} source size ${this.size}"
        println "${sourceString2} source size ${this.size2}"
    }

    def setSourceCRA(String sourceString){
        sourceCRA=CellRangeAddress.valueOf(sourceString)
    }
    def setSourceCRA2(String sourceString){
        sourceCRA2=CellRangeAddress.valueOf(sourceString)
    }

    def getSourceCRA(){
        return sourceCRA
    }

    def getSourceCRA2(){
        return sourceCRA2
    }

    def findColumnEnd(int firstRow, int colnumber,endOfColumnTest){
        int i=firstRow+2
        println firstRow
        println "finding column end ${sheet.getLastRowNum()}"
        Cell cell = sheet.getRow(firstRow+1).getCell(colnumber) //want the first data row not the header
        def origCellType = cell.getCellType()
        println cell.getCellType()
        while (i<sheet.getLastRowNum() ){

            println "row number $i"
            cell = sheet.getRow(i).getCell(colnumber) //want the first data row not the header
            if (endOfColumnTest(cell,origCellType)) {
                i++
            }
            else break

        }
        return i
    }

    def traverse(action){

        println action
        for (int i=sourceCRA.getFirstRow(); i<=sourceCRA.getLastRow();i++){
            Row row = sheet.getRow(i)
            for (int j=sourceCRA.getFirstColumn(); j<=sourceCRA.getLastColumn();j++){
                println "coords ${i},${j}"
                Cell cell = row.getCell(j)
                action.call(cell)

            }
        }
        return sheet

    }

    def traverseCombine(action){ 
        def cells=[:]
        def cellList=[]
        def cellList2=[]
        for (int i=sourceCRA.getFirstRow(); i<=sourceCRA.getLastRow();i++){
            Row row = sheet.getRow(i)
            
            for (int j=sourceCRA.getFirstColumn(); j<=sourceCRA.getLastColumn();j++){
                println "coords ${i},${j}"
                Cell cell = row.getCell(j)
                cellList.add(cell)

            }
        }
            
            for (int i=sourceCRA2.getFirstRow(); i<=sourceCRA2.getLastRow();i++){
            Row row = sheet.getRow(i)

            for (int j=sourceCRA2.getFirstColumn(); j<=sourceCRA2.getLastColumn();j++){
                println "coords ${i},${j}"
                Cell cell = row.getCell(j)
                cellList2.add(cell)

            }
        }
        cells.put("source1",cellList)
        cells.put("source2",cellList2)
        action.call(cells)
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

    def getFirstRow2(){
        return sourceCRA2.getFirstRow()
    }

    def getSize(){
        return this.size
    }
    def getSize2(){
        return this.size2
    }

    def endOfColumnTest ={Cell cell,origCellType ->

        return (cell.getCellType()==origCellType)

    }
	
}

