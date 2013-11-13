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

package org.hits.ui.rdf

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.jena.iri.IRI
import com.hp.hpl.jena.rdf.model.Property
import com.hp.hpl.jena.rdf.model.Statement
import org.apache.poi.ss.usermodel.*
import org.hits.parser.extractExcel.excelReader
import org.hits.ui.Knowledge
import org.hits.ui.Template
import org.hits.ui.Experiment
import org.apache.poi.ss.util.CellReference
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.hp.hpl.jena.datatypes.TypeMapper
import com.hp.hpl.jena.datatypes.xsd.impl.XSDDouble

/**
 *
 * @author rongji
 */
class RdfBuilder {
    private final IRI rootID;
	
   
    String nameSpaceHits="http://hits.org/ontology/lab#"
    String dataTypeSpace="http://www.w3.org/2001/XMLSchema#"
    String DEFAULT_PROPERTY_URN = "${nameSpaceHits}hasAssociatedItem";
	
    def pattern=/(\w+)(\d+)/
    Experiment experiment
    
    RdfBuilder(Experiment experiment){
        this.experiment=experiment 
    }
    
    def exportWorkbookIntoRdf( File outputFile){
        def setupResource=experiment.resources.find{it.type=="setup" && it.state=="active"}
        def gelInspectorResources=experiment.resources.findAll{it.type=="gelinspector" && it.state=="active"}
        println setupResource.fileName
        
        Model model = ModelFactory.createDefaultModel();	
       	model.setNsPrefix("excemplify", "${nameSpaceHits}");
        model.setNsPrefix("xsd", "${dataTypeSpace}");
        Resource root = model.createResource("${nameSpaceHits}Experiment");
        Property  hasIDProperty= createProperty(model, "hasID");
        Property hasNameProperty=createProperty(model, "hasName");
        Property hasTopicProperty=createProperty(model, "hasTopic");
     
        
        root.addProperty(hasNameProperty, model.createLiteral("${experiment.filename}"));
        root.addProperty(hasIDProperty, model.createLiteral("${experiment.id}"));
        root.addProperty(hasTopicProperty, model.createLiteral("${experiment.topic}"));
        
   
            
        
        
        
        InputStream bis = new ByteArrayInputStream(setupResource.binaryData)      
        org.apache.poi.ss.usermodel.Workbook datafileworkbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(bis)
        
        Template setUpTemplate=Template.findByTemplateName(experiment.setUpTemplateName)
        //Template gelTemplate=experiment.stages.find{it.stageName=="gelInspector"}.stageTemplate
        //        Resource setup = model.createResource("${nameSpaceHits}Setup");
        
        setUpTemplate.knowledgeList.each{Knowledge knowledge->
        
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
                            cellList<<cell
                        }
                    
                    }
                }
                
            }
            String knowledgeName=knowledge.knowledgeName
            addMetaStatementNode(root,model, knowledgeName,cellList)
        
        }
        
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
            int endcol=labelRow.getLastCellNum() 
            List<Cell> cellList=[]
            (startrow+1..endrow).each{row->
                Row currentRow=gelsheet.getRow(row) 
                if(currentRow){
                    Cell timeCell=currentRow.getCell(startcol)
                    Cell treatmentCell=currentRow.getCell(startcol+1)
                    Cell cellCell=currentRow.getCell(startcol+2)
                    Cell conditionCell=currentRow.getCell(startcol+3)
                    String index=currentRow.getCell(startcol-1)
                    (startcol+4..endcol).each{col->
                        String proteinString="unknown"
                        Cell proteinCell=labelRow.getCell(col)
                        if(proteinCell){
                            proteinString=parseCell(proteinCell).toString().split("-")[0]
                    
                            println "protein is $proteinString"
                    
                            Cell dataCell=currentRow.getCell(col)
                            def params=[index:"${parseCell(proteinCell).toString()}-${index}", blot:blot, time:"${parseCell(timeCell).toString()}", treatment:"${parseCell(treatmentCell).toString()}", cell:"${parseCell(cellCell).toString()}", protein:proteinString, datapoint:"${parseCell(dataCell).toString()}"]
                            addDataStatementNode(root,model, params) 
                        }
                    }
            
                }
            }  
        }
        
        
        
        
        
        try {
            // model.write(new OutputStreamWriter(outStream,"UTF-8"));
          
            model.write(new FileOutputStream(outputFile));
        } catch (UnsupportedEncodingException e) {
            println e.getMessage()
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
    
    IRI getRootID(){
        return rootID
    }
    private Property getDefaultProperty(Model model) {	
        Property property = model.createProperty(DEFAULT_PROPERTY_URN);						
        return property;		
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
    
    //    private void addNode(Resource rootResource,Model model, String knowledgeName, List<Cell> cells) {
    //   
    //        // if (cellDetails.definesLiteral()) {
    //        addStatementNode(rootResource, model, knowledgeName, cells)
    //        //addLiteralNode(rootResource,model,knowledge, cells);
    //	//	}
    //        // else {
    //        //  addStatementNode(rootResource, model, cell);
    //        //}
    //    }
	
    //    private void addLiteralNode(Resource rootResource,Model model, String knowledgeName, List<Cell> cells) {
    //       
    //   
    //        if(knowledgeName.toLowerCase().startsWith("dose")){
    //            knowledgeName="Treatments"
    //        }
    //        Property property = createProperty(model, "has${knowledgeName}");
    //        cells.each{cell->
    //            rootResource.addProperty(property, model.createLiteral("${String.valueOf(parseCell(cell))}"));   
    //        }
    //
    //    }
    //   def params=[index:"${parseCell(proteinCell).toString()}${index}", blot:blot, time:"${parseCell(timeCell).toString()}", treatment:"${parseCell(treatmentCell).toString()}", cell:"${parseCell(cellCell).toString()}", condition:"${parseCell(conditionCell).toString()}", protein:proteinString, datapoint:"${parseCell(dataCell).toString()}"]
               
    private void addDataStatementNode(Resource rootResource, Model model, Map param){
        println "param $param"
        String blot=param.blot 
        String index=param.index
        String protein=param.protein
        String cell=param.cell
       String treatment=param.treatment
    def datapoint=param.datapoint
     def  time=param.time
 
        
        
        Resource r = model.createResource("${nameSpaceHits}$blot-$index");

        Property hasDataProperty = createProperty(model, "hasData");

        Property hasDataValueProperty=createProperty(model, "hasDataValue");
     
        r.addProperty(hasDataValueProperty, model.createTypedLiteral(datapoint.toString(),XSDDatatype.XSDdecimal));
                            
        Property property = createProperty(model, "hasMetaData");
   
        Property associateWithProteinProperty=createProperty(model, "hasProteins");
       
        Property hasTimeProperty=createProperty(model, "hasTimePoints");
        Property hasTreatmentProperty=createProperty(model, "hasTreatments");
        Property hasCellProperty=createProperty(model, "hasCells");
  
    
     
  
        r.addProperty(associateWithProteinProperty, model.createTypedLiteral("${protein}"));   
        r.addProperty(hasCellProperty, model.createTypedLiteral("${cell}"));   
        r.addProperty(hasTreatmentProperty, model.createTypedLiteral("${treatment}"));  
        r.addProperty(hasTimeProperty, model.createTypedLiteral(time.toString(),XSDDatatype.XSDdecimal));   

        
        Statement s = model.createStatement(rootResource, hasDataProperty, r);
        model.add(s);
    }
	
    private void addMetaStatementNode(Resource rootResource,Model model, String knowledgeName, List<Cell> cells) {
        //		Property property = createProperty(model,cellDetails.getOWLPropertyItem());

        Property property = createProperty(model, "hasMetaData");
        Property hasValueProperty= createProperty(model, "hasValue");
        // Property hasUniteProperty= createProperty(model, "hasUnit");
        // Property hasInfoProperty= createProperty(model, "hasInfo");  
        Property isAProperty= createProperty(model, "isA");
 
        if(knowledgeName.toLowerCase().startsWith("dose")){
            knowledgeName="Treatments"
        }
        
        cells.eachWithIndex{cell, index->
         
            Resource r = model.createResource("${nameSpaceHits}${knowledgeName}$index");
            r.addProperty(hasValueProperty, model.createLiteral("${String.valueOf(parseCell(cell))}"));   
            //r.addProperty(hasUnitProperty, model.createLiteral(hasValueProperty,"${String.valueOf(parseCell(cell))}"));  
            //r.addProperty(hasInfoProperty, model.createLiteral("${String.valueOf(parseCell(cell))}"));   
            r.addProperty(isAProperty, model.createLiteral("${knowledgeName}"));   
            
            Statement s = model.createStatement(rootResource, property, r);
            model.add(s);
        
        }
       
    }
    
    private Property createProperty(Model model, String propertyItem) {
        Property property;
        if (propertyItem==null) {
            property = getDefaultProperty(model);
        }
        else {
            property = model.createProperty("${nameSpaceHits}${propertyItem}");						
        }				
		
        return property;
    }
}

