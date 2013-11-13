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

package org.hits.ui.labbook

import org.hits.ui.Experiment
import org.hits.ui.Knowledge
import org.hits.ui.Template
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellReference


/**
 *
 * @author rongji
 */
class LabBookSnippetBuilder {

	
    def pattern=/(\w+)(\d+)/
    Experiment experiment 
    String htmlString
    
    LabBookSnippetBuilder(Experiment experiment){
        this.experiment=experiment 
        htmlString=""
    }
    //    def convertIntoTexFile(File htmlFile, File texFile){
    //             Parser parser = new Parser();
    //            parser.parse(new File(htmlFile),
    //                     new ParserHandler(new File(texFile)));
    //    }
    
 
    
    def generateSnippet(){
        byte[] bytes
        File outputFile= File.createTempFile("snippet", ".html");
        println outputFile.absolutePath
        def setupResource=experiment.resources.find{it.type=="setup" && it.state=="active"}
        def gelInspectorResources=experiment.resources.findAll{it.type=="gelinspector" && it.state=="active"}
        println setupResource.fileName
        htmlString="<html><head><title>LabBook Snippet For Experiment ${experiment.filename}</title></head><body><h1>Experiment: ${experiment.filename}  Starts At: ${experiment.createdOn}</h1> <h3><font color='darkblue'>Techniques:</font> ${experiment.topic}</h3>  <h3><font color='darkblue'>Experimentalist:</font> ${experiment.author.name}</h3>  <h3><font color='darkblue'>Set Up</font></h3>"
         
        InputStream bis = new ByteArrayInputStream(setupResource.binaryData)      
        org.apache.poi.ss.usermodel.Workbook datafileworkbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(bis) 
        
        Template setUpTemplate=Template.findByTemplateName(experiment.setUpTemplateName)
   
        
        setUpTemplate.knowledgeList.each{Knowledge knowledge->
            htmlString=htmlString+"<p><b>${knowledge.knowledgeName} :</b><p>"
            Sheet currentsheet=datafileworkbook.getSheetAt(knowledge.sheetIndex as int)
            int startcol
            int startrow
            int endcol
            int endrow
            if(knowledge.markCellRange.indexOf(":")!=-1){
            
                String upperleft=knowledge.markCellRange.tokenize(":").first()
      
                String downright=knowledge.markCellRange.tokenize(":").last()
     
                def matcher = "$upperleft" =~ pattern
    
                startcol=CellReference.convertColStringToIndex("${matcher[0][1]}")
                startrow= matcher[0][2] as int
                //        
                def matcher2 = "$downright" =~ pattern
    
                endcol=CellReference.convertColStringToIndex("${matcher2[0][1]}")
                endrow= matcher2[0][2] as int
       
            }else{
                def matcher = "${knowledge.markCellRange}" =~ pattern 
                startcol=CellReference.convertColStringToIndex("${matcher[0][1]}")
                startrow= matcher[0][2] as int 
                endcol=startcol
                endrow = findColumnEnd(currentsheet, startrow, startcol)     
            }
            
            List<Cell> cellList=[]
            (startrow..endrow).each{num->
                Row row=currentsheet.getRow(num)
                if(row){
                    (startcol..endcol).each{col->
                        Cell cell=row.getCell(col)
                        if(cell!=null && parseCell(cell)!=""){
                            htmlString=htmlString+"${parseCell(cell)};  "
                            cellList<<cell
                        }
                    
                    }
                }
                
            }
            htmlString=htmlString+"</p>"
            String knowledgeName=knowledge.knowledgeName
             
            //addMetaStatementNode(root,model, knowledgeName,cellList)
        
        }
          htmlString=htmlString+"<h3><font color='darkblue'>Loading Details</font></h3>"
                if(gelInspectorResources){
                gelInspectorResources.each{gelResource->
                    InputStream bisgel = new ByteArrayInputStream(gelResource.binaryData)      
                    org.apache.poi.ss.usermodel.Workbook gelworkbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(bisgel) 
                    Sheet gelsheet=gelworkbook.getSheetAt(0)
                    String sheetName=gelsheet.getSheetName()
                    String blot=sheetName.getAt(sheetName.length()-1)
                    println "blot $blot"
                    int startrow=0
                    int startcol=1
                    int endrow=findColumnEnd(gelsheet, startrow, startcol) 
                    Row labelRow=gelsheet.getRow(startrow)
                    //int endcol=labelRow.getLastCellNum() 
                    List<Cell> cellList=[]
                        htmlString=htmlString+"<p><b>Blot: $blot</b></p>"
                    (startrow+1..endrow).each{row->
                        Row currentRow=gelsheet.getRow(row) 
                        if(currentRow){
                            Cell timeCell=currentRow.getCell(startcol)
                            Cell treatmentCell=currentRow.getCell(startcol+1)
                            Cell cellCell=currentRow.getCell(startcol+2)
                            Cell conditionCell=currentRow.getCell(startcol+3)
                            String index=currentRow.getCell(startcol-1)
                            htmlString=htmlString+"<p>${parseCell(timeCell).toString()};  ${parseCell(treatmentCell).toString()}; ${parseCell(cellCell).toString()}</p>"

                        }
                    }  
                }
                }else{
                htmlString=htmlString+" <p><font color='red'> Do you forget to press the GelInspector button to generate/re-generate??</font></p>"     
                }    
         htmlString=htmlString+"</body></html>"
        
        println(htmlString)
        
        try {
            FileOutputStream out=new FileOutputStream(outputFile)
            OutputStreamWriter writer=new OutputStreamWriter(out)
            writer.write(htmlString)
            writer.close()
            InputStream inputStream = new FileInputStream(outputFile);

            bytes = new byte[(int)outputFile.length()];
            inputStream.read(bytes);

            inputStream.close();
            outputFile.delete()
            return bytes;
            
       
        } catch (UnsupportedEncodingException e) {
            println e.getMessage()
            return null
        }
        //        excelReader excel=new excelReader(datafileworkbook)
        //        excel.fetchValidationTerms()
    }
     
    private boolean isRowEmpty(Row row) {
        
        def empty=true
    
        for (int c = 0; c <= row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK){
                empty=empty&&false
            }
          
        }
        return empty;
    }
    def findColumnEnd(Sheet sheet, int firstRow, int colnumber){
        int i=firstRow
        //println "first row $firstRow"

        int  lastEmptyRow=firstRow
        def rowIterator = sheet.rowIterator()
        while(rowIterator.hasNext()) {
            def row = rowIterator.next()
            if(!isRowEmpty(row)){
                lastEmptyRow=row.getRowNum()+1 
            }
              
        }
        // println "finding column end ${lastEmptyRow}"
        return lastEmptyRow
    }
    
   
    def parseCell(cell){
        // println "here SetupExcelParser parseCell"
        if(cell){
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
                    return ""
           
                }
            }
        }else{
            return "not defined"
        }
    }	

    
	
}

