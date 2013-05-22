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

package org.hits.parser.extractExcel

/**
 *
 * @author rongji
 */

import groovy.xml.MarkupBuilder
import groovy.util.IndentPrinter
import org.apache.poi.ss.util.CellRangeAddress




import java.util.*
import org.apache.poi.hssf.usermodel.HSSFDataValidation
import org.apache.poi.hssf.usermodel.DVConstraint
import org.apache.poi.ss.usermodel.*
import org.apache.poi.hssf.usermodel.HSSFSheet
//import org.apache.poi.hssf.util.CellRangeAddress

class excelReader {
    def workbook
    def numberOfSheet

    def labels
    def row
    def mergedAreas=[:]

    static final String VALIDATION_SHEET_PREFIX = "wksowlv";

    def ONTOLOGY_ROW_KEY="ontology";
    def validationTypes=["DIRECTSUBCLASSES", "SUBCLASSES", "INDIVIDUALS","DIRECTINDIVIDUALS"]

    Map annotationList=new HashMap()
    Map annotationTypeList=new HashMap()
    def sheetAnnotationList=new HashSet()
    Map locations=new HashMap()
	  
    excelReader(File file) {
        
        println "reading ${file.getName()} "
       
        workbook=WorkbookFactory.create(file) 
        numberOfSheet=workbook.getNumberOfSheets();


    }
    

    
    def getValidations(HSSFSheet sheet) {
        def validationList = new ArrayList<Validation>();
        //def patchPOI=new patchPOI()
       
        for (HSSFDataValidation validation : getValidationData(sheet)) {

            DVConstraint constraint=validation.getConstraint()
         
            for (CellRangeAddress address : validation.getRegions().getCellRangeAddresses()) {
                
                def record=new Validation(validation.getConstraint().getFormula1(), sheet.getSheetName(), address.getFirstColumn(), address.getLastColumn(), address.getFirstRow(), address.getLastRow())
                println "(${record.fromRow},${record.fromColumn})->(${record.toRow},${record.toColumn}) ${record.sheet} ${record.list}"
                validationList.add(record);
                
            }

        }
        return validationList;
    }
    public List<HSSFDataValidation> getValidationData(HSSFSheet sheet) {    	    	
    	return PatchedPoi.getInstance().getValidationData(sheet, workbook);
    }

    
    def getCellValue(Cell cell){

       // println "type ${cell.getCellType()}"      
        switch (cell.getCellType()) {                
        case Cell.CELL_TYPE_STRING:   
            String value
            String newvalue      
            value= cell.getStringCellValue().toString().replaceAll(/\n\[/,"(")      
            newvalue=value.replaceAll(/\]/,")")        
            return newvalue
        case Cell.CELL_TYPE_NUMERIC:
            return cell.getNumericCellValue()
        case Cell.CELL_TYPE_BOOLEAN:
            return cell.getBooleanCellValue()
        case Cell.CELL_TYPE_FORMULA:
            return cell.getCellFormula()
        case Cell.CELL_TYPE_ERROR:
            return cell.getErrorCellValue()
        case Cell.CELL_TYPE_BLANK:           
            return ""
           
        }
       
     
    }
    


    def getMergedRegions(sheet){
       
        def numberOfMergedArea=sheet.getNumMergedRegions()
        print "this sheet has $numberOfMergedArea merged area"
        (0..< numberOfMergedArea).each{regionI->
            def  cellRangeAddress=sheet.getMergedRegion(regionI)
            int rangeR=cellRangeAddress.getLastRow()-cellRangeAddress.getFirstRow()
            int rangeC=cellRangeAddress.getLastColumn()-cellRangeAddress.getFirstColumn()

            MergedArea area=new MergedArea()
            area.firstCol=cellRangeAddress.getFirstColumn()
            area.lastCol=cellRangeAddress.getLastColumn()
            area.colRange=rangeC+1 //colspan
            area.firstRow=cellRangeAddress.getFirstRow()
            area.lastRow=cellRangeAddress.getLastRow()
            area.rowRange=rangeR+1 //rowspan
            print  area.firstRow+" "+rangeR+" "+area.firstCol+" "+rangeC

            mergedAreas.put("$area.firstRow:$area.firstCol",area)

        }
        return mergedAreas
    }
	  
    def getSheet(idx) {
        def sheet

        if(! idx) idx = 0
        if(idx instanceof Number) {
            sheet = workbook.getSheetAt(idx)
        } else if(idx ==~ /^\d+$/) {
            sheet = workbook.getSheetAt(Integer.valueOf(idx))
        } else {
            sheet = workbook.getSheet(idx)
        }

        return sheet
    }
	  
//    def cell(idx) {
//        if(labels && (idx instanceof String)) {
//            idx = labels.indexOf(idx.toLowerCase())
//        }
//        return row[idx]
//    }
//	  
//    def propertyMissing(String name) {
//        cell(name)
//    }

    def traverseMergedRegions(Map map){
        def skipList=[];

        map.each{k,v->
            MergedArea merg=v
            (merg.firstRow..merg.lastRow).each{ row->
                (merg.firstCol..merg.lastCol).each{col->
                    print " $row:$col "
                    if((row==merg.firstRow)&&(col==merg.firstCol)){
                       
                    }else{
                        skipList.add("$row:$col")
                    }
                }
            }
        }
        print skipList
        return skipList
    }

    def excelXmlStringBuilderToDo(){
        // print "numberOfSheet $numberOfSheet"
        def writer=new StringWriter()
        def xml=new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
        xml.DOCUMENTS(){   
            def sheet
            def mergedRegionsMap
            def skipList
            (0..< numberOfSheet).each{currentSheetIndex ->
                print "currentSheetIndex $currentSheetIndex"
                sheet = getSheet(currentSheetIndex)
                mergedRegionsMap=getMergedRegions(sheet)
                skipList=traverseMergedRegions(mergedRegionsMap)          
                def title=sheet.getSheetName();
                def rows=sheet.getLastRowNum()+1+1;
                def cols=0;
                def rowIterator = sheet.rowIterator()
                while(rowIterator.hasNext()) {
                    row = rowIterator.next()
                    cols=row.getLastCellNum()>cols? row.getLastCellNum():cols
                }
                
                if(cols>0){
                    DOCUMENT(title:"$title"){
                        METADATA(){
                            COLUMNS("$cols")
                            ROWS("$rows")
                        }
                        DATA(){
                            (0..< rows).each{currentRowIndex ->
                 
                                def currentRow=sheet.getRow(currentRowIndex)
                                if (currentRow!=null){
                                   "R$currentRowIndex"(){
                                        for(def currentIndex=0; currentIndex<cols; currentIndex++){
                                            MergedArea area
                                            def colSpan
                                            def rowSpan
                                            def currentCell=currentRow.getCell(currentIndex)
                                            def flag=false
                                            if(!skipList.contains("$currentRowIndex:$currentIndex")){
                        
                                                area=mergedAreas.get("$currentRowIndex:$currentIndex")
                                                if (area!=null){  //merged area appears
                                                    rowSpan=area.rowRange
                                                    colSpan=area.colRange
                                                    flag=true
                                      
                                                }
                                                if(currentCell!=null){
                                                    if(flag){
                                                        "C$currentIndex"(rowspan:"$rowSpan", colspan:"$colSpan","$currentCell")
                                                    }else{
                                                       "C$currentIndex"("$currentCell")  
                                                    }
                                                }
                                                else{
                                                    if(flag){
                                                         "C$currentIndex"(rowspan:"$rowSpan", colspan:"$colSpan")
                                                    }else{
                                                        "C$currentIndex"()
                                                    }

                                                }
                            
                                            }else{
                                                if(currentRowIndex==rows){
                                                 "C$currentIndex"()
                                                }else{
                                                 "C$currentIndex"("please skip")
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        //  print writer.toString()
        return writer.toString()
        
    }
    
    def fetchValidationTerms(){
        (0..< numberOfSheet).each{currentSheetIndex ->
            def sheet = getSheet(currentSheetIndex)
            if(sheet instanceof HSSFSheet){   
                println "it is a HSSFSheet"
                def validationList=getValidations(sheet) 
                if(validationList){               
                    validationList?.each{annotationList.put("${it.list}", []); sheetAnnotationList<< it.sheet; locations.put("${it.fromRow}and${it.fromColumn}",it.list)}
                    println annotationList 
                    println  sheetAnnotationList
                    println  locations
                }
                def title=sheet.getSheetName();
                if(annotationList.containsKey("$title")){
                    def rows=sheet.getLastRowNum()+1;
              
                    def cols=0;
                    def rowIterator = sheet.rowIterator()
                    while(rowIterator.hasNext()) {
                        def row = rowIterator.next()
                        cols=row.getLastCellNum()>cols? row.getLastCellNum():cols

                    }
                    if(cols>0){
                        def type
                        (0..< rows).each{currentRowIndex ->
                            def currentRow=sheet.getRow(currentRowIndex)
                            if (currentRow!=null){
                           
                                for(int currentIndex=0; currentIndex<currentRow.getLastCellNum(); currentIndex++){
                   
                                    def currentCell=currentRow.getCell(currentIndex)
                                    if(currentCell!=null){
                                        if(validationTypes.contains(currentCell.toString())){
                                            type=currentCell
                                            //                                        println"type: $type"
                                            currentIndex=currentIndex+1
                                            def annotationCell=currentRow.getCell(currentIndex)
                                            //                                        println"annotation:$annotationCell"
                                        }else if(currentCell.toString().equals(ONTOLOGY_ROW_KEY)){
                                            currentIndex=currentIndex+1
                                            def keyCell=currentRow.getCell(currentIndex)
                                            currentIndex=currentIndex+1
                                            def keyFileCell=currentRow.getCell(currentIndex)
                                         
                                            //                                        println"key: $keyCell : $keyFileCell"
                                      
                                        }else if(currentCell.toString().contains("#")){ 
                                            def optionannotation=currentCell.toString().split("#")[1].split(">")[0]
                              
                                            currentIndex=currentIndex+1
                                            def optionCell=currentRow.getCell(currentIndex)
                                            //                                        println "option: $optionCell"  
                                            //                                        
                                            //                                        println "hiddenkey $title $type"
                                            annotationList.get("$title")<<"$optionCell($optionannotation)"
                                            annotationTypeList.put("$title", "$type")
                                        }
                            
                                                                   
                                    }
                                }
                            
                            }        
                        }
                    }
                }
            }else{
                println " it is not a HSSFSheet, we can not fetch the validation term! sorry"
            }          
        }
    }
    def excelXmlBuilder(){
        println "xmlbuilding"
        def writer=new StringWriter()
        def xml=new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
 
        fetchValidationTerms()
        
        
        xml.DOCUMENTS(){
            (0..< numberOfSheet).each{currentSheetIndex ->
                def sheet = getSheet(currentSheetIndex)
                 
                def title=sheet.getSheetName();
             
                boolean flag=false;
                def rows=sheet.getLastRowNum()+1;
              
                def cols=0;
                def rowIterator = sheet.rowIterator()
                while(rowIterator.hasNext()) {
                    def row = rowIterator.next()

                    cols=row.getLastCellNum()>cols? row.getLastCellNum():cols
                }
                if(!annotationList.containsKey("$title")){
                    if(cols>0){
             
                        DOCUMENT(title:"$title"){
                            METADATA(){
                                COLUMNS("$cols")
                                ROWS("$rows")
                            }
                            DATA(){
                                (0..< rows).each{currentRowIndex ->
                                    
                                    def currentRow=sheet.getRow(currentRowIndex)
                                    if (currentRow!=null){
                                "R$currentRowIndex"(){
                                            (0 .. currentRow.getLastCellNum()-1).each{currentIndex->
                                                Cell currentCell=currentRow.getCell(currentIndex)
                                                if(currentCell!=null){
                                                    
                                                    if(locations.containsKey("${currentRowIndex}and${currentIndex}")){
                                                        def hiddenkey=locations.get("${currentRowIndex}and${currentIndex}")
                                                        def options=annotationList.get("$hiddenkey")
                                                        def optiontype=annotationTypeList.get("$hiddenkey")
                                                        String optionsString="'"
                                                        optionsString=optionsString+options.join('\',\'')
                                                        optionsString=optionsString+"','$optiontype'"
                                                        // println optionsString
                                                        
                                                        // println "contains"
                                                    "C$currentIndex"("=DROPDOWN($optionsString)")   
                                                    }else{
                                                        def cellValue=getCellValue(currentCell)
                                                 "C$currentIndex"("$cellValue")    
                                                    }  
                                                                   
                                                }else{
                                                
                                           "C$currentIndex"("")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
       
                        }
                    }
                }
  
            }
        }
            
        

        print writer.toString()
            
        return writer.toString()
          
        
    }
        
        
  
}




