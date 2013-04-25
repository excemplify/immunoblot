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
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFCell
import org.apache.poi.hssf.usermodel.HSSFDateUtil
import groovy.xml.MarkupBuilder
import groovy.util.IndentPrinter
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress




import java.util.*
import org.apache.poi.hssf.usermodel.HSSFDataValidation
import org.apache.poi.hssf.usermodel.DVConstraint
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
	  
    excelReader(String fileName) {
        HSSFRow.metaClass.getAt = {int idx ->
            def cell = delegate.getCell(idx)
            if(! cell) {
                return null
            }
            def value
            switch(cell.cellType) {
            case HSSFCell.CELL_TYPE_NUMERIC:
                if(HSSFDateUtil.isCellDateFormatted(cell)) {
                    value = cell.dateCellValue
                } else {
                    value = cell.numericCellValue
                }
                break
            case HSSFCell.CELL_TYPE_BOOLEAN:
                value = cell.booleanCellValue
                break
               
            default:
                value = cell.stringCellValue
                break
            }
            return value
        }
	  
        new File(fileName).withInputStream{is->
            workbook = new HSSFWorkbook(is)
            numberOfSheet=workbook.getNumberOfSheets();
        }

    }
    
    excelReader(byte[] binaryData){

        ByteArrayInputStream bis = new ByteArrayInputStream(binaryData)     
        workbook = new HSSFWorkbook(bis)
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

    
    def getCellValue(HSSFCell cell){
        def value
        switch(cell.cellType) {
        case HSSFCell.CELL_TYPE_NUMERIC:
            if(HSSFDateUtil.isCellDateFormatted(cell)) {
                value = cell.dateCellValue
            } else {
                value = cell.numericCellValue
            }
            break
        case HSSFCell.CELL_TYPE_BOOLEAN:
            value = cell.booleanCellValue
            break
        default:
            value = cell.stringCellValue
            break
        }
        return value
    }
    //
    //
    //    /**
    //     *convention=[sheetName:"setup", labelSign:"#", finishCondition:HSSFCell.CELL_TYPE_BLANK]
    //     *
    //     *convention1:sheet name must contains "setup"
    //     *convention2: set up information category must starts with "#"
    //     *convention3: the process of set up information category stops when the blank cell occurs
    //     *
    //     *implicit convention: the label (after labelSign) match the predefined category name in the loading experiment model
    //     */
    //
    //    def processSheetAgainstConvention (HSSFSheet sheet, Map convention=[sheetName:"setup", labelSign:"#", finishCondition:HSSFCell.CELL_TYPE_BLANK]){ //if the cells start with #
    //        Map setUpMap=[:]
    //        if(sheet.getSheetName().toLowerCase().trim().indexOf(convention.sheetName)!=-1){
    //            def labelFlag=false
    //            def lastRowNum=sheet.getLastRowNum()
    //            (0..lastRowNum).each{currentRow->
    //                if(labelFlag){
    //                    return
    //                }else{
    //                    HSSFRow row=sheet.getRow(currentRow)
    //                    def lastColNum=row.getLastCellNum()
    //                    (0..lastColNum).each{currentCol->
    //                        HSSFCell cell=row.getCell(col)
    //                        if (cell.cellType==HSSFCell.CELL_TYPE_STRING){
    //                            String cellValue=cell.stringCellValue
    //                            if (cellValue.trim().startsWith(convention.labelSign)){
    //                                labvelFlag=true
    //                                def category=cellValue.trim().split(convention.labelSign)[1]
    //                                def subList=getColumnContentUntilFinishCondition(sheet, currentRow, currentCol, convention.finishCondition)
    //                                setUpMap.put(category, subList)
    //                                print "category $category $convention.sheetName starts at row $currentRow col $currentCol vertically"
    //
    //                            }
    //
    //                        }
    //
    //                    }
    //
    //                }
    //
    //            }
    //
    //            if(!labelFlag){
    //                println "The entire sheet was processed, do not find labelSign $convention.labelSign"
    //            }
    //        }else{
    //            println "What you try to process is not a kid of $convention.sheetName file"
    //        }
    //        return setUpMap
    //    }
    //
    //
    //    def getColumnContentUntilFinishCondition(HSSFSheet sheet, int startRow, int startCol, int finishCondition){  //when cell.cellType=finishCondition stop
    //        def columnContent=[]
    //        def continueFlag=true
    //        def rowNum=startRow+1
    //        def colNum=startCol
    //        while (continueFlag){
    //            def cell=sheet.getRow(rowNum).getCell(colNum)
    //            if(cell!=null){
    //                if(cell.cellType!=finishCondition){
    //                    columnContent.add(getCellValue(cell))
    //                    rowNum++
    //                }else{
    //                    println "finish condition fullfiled"
    //                    continueFlag=false
    //                }
    //            }else{
    //                println "null cell"
    //                continueFlag=false
    //            }
    //
    //        }
    //
    //        return columnContent
    //    }


    def getMergedRegions(HSSFSheet sheet){
       
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
	  
    def cell(idx) {
        if(labels && (idx instanceof String)) {
            idx = labels.indexOf(idx.toLowerCase())
        }
        return row[idx]
    }
	  
    def propertyMissing(String name) {
        cell(name)
    }

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
            HSSFSheet  sheet = getSheet(currentSheetIndex)
                     
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
                   
        }
    }
    def excelXmlBuilder(){
        println "xmlbuilding"
        def writer=new StringWriter()
        def xml=new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
 
        fetchValidationTerms()
        
        
        xml.DOCUMENTS(){
            (0..< numberOfSheet).each{currentSheetIndex ->
                HSSFSheet  sheet = getSheet(currentSheetIndex)
      
          
                 
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
                                                def currentCell=currentRow.getCell(currentIndex)
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
                                                 "C$currentIndex"("$currentCell")    
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
                
                //                }else{
                //                    def valuelist
                //                                 (0..< rows).each{currentRowIndex ->
                //                                    def currentRow=sheet.getRow(currentRowIndex)
                //                                    if (currentRow!=null){
                //                                //"R$currentRowIndex"(){
                //                                            (0 .. currentRow.getLastCellNum()-1).each{currentIndex->
                //                                                def currentCell=currentRow.getCell(currentIndex)
                //                                                if(currentCell!=null){
                //                                                    String value=currentCell.toString()    
                //                                                
                //                                    if(!value.contains("&lt")&& !value.contains("&gt") && !value.equals("ontology")){
                //                                        valuelist.add(value)
                //                                    }   
                //                                                
                ////                                           "C$currentIndex"("$currentCell")                                   
                ////                                                }else{
                ////                                           "C$currentIndex"("")
                ////                                                }
                //                                            }
                //                                       // }
                //                                    }
                //                                }
                //                }  
                //
            }
        }
            
        

        print writer.toString()
            
        return writer.toString()
          
        
    }
        
        
  
        
        
    
   

    //    def processSetUpWorkBook(){
    //        (0..< numberOfSheet).each{currentSheetIndex ->
    //            def sheet=getSheet(currentSheet)
    //            def firstRow=sheet.getRow(0)  //assume the first row contains the label info
    //            def signallCell=firstRow.getCell(0).toString()
    //            switch(signallCell){
    //                case 'IMMUNOBLOT_SETUP':
    //                println "loading IMMUNOBLOT_SETUP"
    //
    //                break
    //
    //                default:
    //                println "$signatureCell"
    //            }
    //
    //
    //
    //            if(firstRow!=null){
    //                (0 .. firstRow.getLastCellNum()-1).each{currentIndex->
    //                    def currentCell=currentRow.getCell(currentIndex)
    //                    if(currentCell!=null){
    //
    //                             //check model
    //                             //
    //                    }
    //
    //                }
    //
    //
    //
    //            }
    //        }
    //    }


    //    def eachLine(Map params = [:], Closure closure) {//open/Closure closure
    //        print "params $params"
    //        def offset = params.offset ?: 0
    //        def max = params.max ?: 9999999
    //        print "numberOfSheet $numberOfSheet"
    //        def currentSheet=params.sheet>numberOfSheet? 0: (params.sheet-1)
    //        def sheet = getSheet(currentSheet)
    //        def rowIterator = sheet.rowIterator()
    //        def linesRead = 0
    //
    //        if(params.labels) {
    //            labels = rowIterator.next().collect{it.toString().toLowerCase()}
    //        }
    //        offset.times{ rowIterator.next() }
    //
    //        closure.setDelegate(this)
    //
    //        while(rowIterator.hasNext() && linesRead++ < max) {
    //            row = rowIterator.next()
    //            closure.call(row)
    //        }
    //    }
	  
}




