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
package org.hits.ui

import org.hits.parser.extractExcel.*
import org.hits.ui.*
import org.hits.parser.ui.template.*
import org.hits.parser.excelimp.*
import org.hits.parser.*
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.hibernate.exception.ConstraintViolationException



/**
 *
 * @author rongji
 */


class AdminController {
    def springSecurityService
    
    def index() { log.info "admin index" }
    
    def buildKnowledge={
        log.info "build knowledge ${params.file}"
        def file=params.file;
        def  knowledgeInstance=new Knowledge(knowledgeName:"New Concept", columnName:params.columnName, firstRow:params.row, firstCol:params.col, lastRow:params.row, lastCol:params.col, sheetIndex:params.sheetIndex, fileName:params.file, markCellRange:params.location, markColor:"color");
        knowledgeInstance.save();
        def templateKnowledgeList =Knowledge.findAllByFileName(file);
        log.info "knowledge count:"+Knowledge.findAllByFileName(file);
        render(template:"/ui/admin/knowledge", model:[templateKnowledgeList:Knowledge.findAllByFileName(file)] )

    }
    def updateKnowledge={
        log.info "update knowledge ${params.file}"
        Knowledge currentKnowledge=Knowledge.find{markCellRange == params.location;fileName==params.file};
        currentKnowledge.knowledgeName=params.knowledgeName;
        currentKnowledge.markColor=params.knowledgeClass;
        currentKnowledge.save(flush:true)
        render(text: """<script type="text/javascript"> updateKnowledge("$currentKnowledge.firstRow","$currentKnowledge.firstCol","$currentKnowledge.lastRow","$currentKnowledge.lastCol","$currentKnowledge.sheetIndex","$currentKnowledge.markColor"); </script>""", contentType: 'text/javascript')
        render(template:"/ui/admin/knowledge", model:[templateKnowledgeList:Knowledge.findAllByFileName("$currentKnowledge.fileName")] )
    }
    def saveTemplatePre={
        render(text: """<script type="text/javascript"> choosePurpose() </script>""", contentType: 'text/javascript')
    }  
    def saveTemplate={
        def purpose=params.purpose
        log.info "purpose $purpose"
        def fileName= session.getAt("openFileName");
        File file=  session.getAt("file");
        if(fileName && file){
            def list =Knowledge.findAllByFileName(fileName);             
            def templateInstance      
            try{
                //create the template instance
                templateInstance=new Template(templateName:fileName, binaryFileData:file.bytes,  knowledgeList:list, type:"public", purpose:purpose).save(failOnError: true);        
                log.info "template $Template.count";
          
            }catch(Exception e){
                session.removeAttribute("openFileName")
                session.removeAttribute("xml")
                session.removeAttribute("templateType")
                session.removeAttribute("file")
                file.delete()   
                log.error e.getMessage()                
                log.info "$fileName already exist."
                if(e instanceof ConstraintViolationException){
                    flash.message = "${e.getConstraintName()}" 
                }else{
                    flash.message="${e.getMessage()}"
                }     
                render(text: """<script type="text/javascript"> warningMessage("$flash.message") </script>""", contentType: 'text/javascript')
                render(template:"/ui/admin/warningMessage")
            }
            file.delete()   
            session.removeAttribute("openFileName")
            session.removeAttribute("xml")
            session.removeAttribute("file")
            session.removeAttribute("templateType")
            render(text: """<script type="text/javascript"> alert("$fileName saved !"); </script>""", contentType: 'text/javascript')
            /**
             *move the following to experiment initialization so that it is more flexible to choose customize stage-specific template
             *rather than just setup template
             */
            
            //            try{
            //                if(templateInstance && purpose=="setup"){       
            //                    def laneloadingTemplateNew=Template.findByTemplateName("laneloading_template_new.xls") //default
            //                    def gelInspectorTemplate=Template.findByTemplateName("gelInspectorTemplate")         //default
            //                    def testRawDataTemplate=Template.findByTemplateName("rawdata_template.xls")   //default but this one later could also be customized
            //  
            //                    // create the ParserDef instances
            //          
            //                    def templateToParserDefInstance=TemplateToParserDef.findByTemplate(templateInstance)
            //                    
            //                    if(templateToParserDefInstance){
            //               
            //                        deleteTemplateToDefs(templateInstance)
            //                    
            //                    }
            //                    
            //                    /**
            //                     *following is default sequence of default parsers in loading, rawdata amd gelinspector stages, you could change it later during experiment initialiate
            //                     **/
            //                    
            //                    def setupToLanesParserDefInstance=ParserDef.findByName("${fileName}SetupToLane")
            //                    def rawDataToGelInspectorDefInstance=ParserDef.findByName("${fileName}RawDataToGelInspDef")
            //                    def rawTextDataToGelInspectorDefInstance=ParserDef.findByName("${fileName}TextRawDataToGelInspDef")
            //                    
            //                    if(setupToLanesParserDefInstance ){
            //                  
            //                        deleteDef(setupToLanesParserDefInstance)
            //                    }else{
            //                        setupToLanesParserDefInstance = new ParserDef(name:"${fileName}SetupToLane", nextStageName: "Lane Setup", parserConfigurations:[
            //                                new ParserConfiguration(sources:[new ExcelSourceDef(knowledgeComment:"Cells",sourceType:"OUTERPRODUCT",cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(templateInstance, 'Cells')}",sheetNum:0,sheetName:"Setup",template:templateInstance),
            //                                        new ExcelSourceDef(knowledgeComment:"TimePoints",sourceType:"OUTERPRODUCT",cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(templateInstance, 'TimePoints')}",sheetNum:0,sheetName:"Setup",template:templateInstance),
            //                                        new ExcelSourceDef(knowledgeComment:"Doses",sourceType:"OUTERPRODUCT",cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(templateInstance, 'Doses')}",sheetNum:0,sheetName:"Setup",template:templateInstance)], 
            //                                    action:ImmunoParserAction.OUTERPRODUCT_COLUMNS_RANDOMIZE.toString(),
            //                                    parserType:"SetupExcelParser",
            //                                    name:"cellsAndTime", 
            //                                    nodeType:"root", 
            //                                    target: new TargetDef(cellRange:"b2:b2" ,sheetNum:0, sheetName:"auto_Loading",name:"Lane setup target", template:laneloadingTemplateNew))])
            //                    }
            //                    if(rawDataToGelInspectorDefInstance){
            //                     
            //                        deleteDef(rawDataToGelInspectorDefInstance)
            //                   
            //                    }else{
            //                        rawDataToGelInspectorDefInstance = new ParserDef(name:"${fileName}RawDataToGelInspDef", nextStageName: "Raw Data to Gel Inspector", parserConfigurations:[
            //                                new ParserConfiguration(sources:[new ExcelSourceDef(sourceType:"CURRWORKBOOK",cellRange:"a2:b2",sheetNum:2, sheetName:"auto_Loading",template:laneloadingTemplateNew), //the lane numbers from the current workbook
            //                                        new ExcelSourceDef(knowledgeComment:"Cells",sourceType:"CURRWORKBOOK", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(templateInstance, 'Cells')}", sheetNum:0, sheetName:"Setup",template:templateInstance), //cell names
            //                                        new ExcelSourceDef(knowledgeComment:"TimePoints",sourceType:"CURRWORKBOOK", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(templateInstance, 'TimePoints')}", sheetNum:0, sheetName:"Setup",template:templateInstance),
            //                                        new ExcelSourceDef(knowledgeComment:"Doses",sourceType:"CURRWORKBOOK", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(templateInstance, 'Doses')}", sheetNum:0, sheetName:"Setup",template:templateInstance)], //time points. These last 2 are used together to find the lane number from prev thing
            //                                    action:ImmunoParserAction.MATCH_AND_SPLIT.toString(),
            //                                    parserType:"RawDataExcelParser",
            //                                    name:"cellsTimeStim",
            //                                    nodeType:"root",
            //                                    target: new TargetDef(cellRange:"a2:c2",sheetNum:0, sheetName:"Gel Inspector", name:"cellsTimeLane", template: gelInspectorTemplate)),
            //                                new ParserConfiguration( sources:[new ExcelSourceDef(sourceType:"Simple", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(testRawDataTemplate, 'Lanes')}", sheetNum:0,sheetName:"not_used",template:testRawDataTemplate),//lane
            //                                        new ExcelSourceDef(sourceType:"Simple", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(testRawDataTemplate, 'Volumes')}", sheetNum:0,sheetName:"",template:testRawDataTemplate), //volume
            //                                        new ExcelSourceDef(sourceType:"Simple", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(testRawDataTemplate, 'Bands')}",sheetNum:0,sheetName:"",template:testRawDataTemplate)],// band Number
            //                                    action:ImmunoParserAction.COPY.toString(),
            //                                    parserType:"RawDataExcelParser",
            //                                    name:"laneAndVolume",
            //                                    nodeType:"down",
            //                                    target:new TargetDef(cellRange:"a2:c2",sheetNum:0, sheetName:"Gel Inspector", name:"RawDataLane", template:gelInspectorTemplate ))])
            //                    }
            //                    //                    //this should change, new strategy is to transform text file to a new raw data spreadsheet.
            //                    if(rawTextDataToGelInspectorDefInstance){
            //                        deleteDef(rawTextDataToGelInspectorDefInstance)   
            //                    }else{
            //                        rawTextDataToGelInspectorDefInstance = new ParserDef(name:"${fileName}TextRawDataToGelInspDef",nextStageName: "Raw Data (Text) to Excel", parserConfigurations:[
            //                                new ParserConfiguration(sources:[new TextFileSourceDef(dataLabel:"Lanes", sourceType:"rawdatatext", startRegexp:"Lane",endRegexp:"r1"),
            //                                        new TextFileSourceDef(dataLabel:"Volume", sourceType:"rawdatatext", startRegexp:"r",endRegexp:"Sum")],
            //                                    parserType:"NonExcelRawDataParser", action:ImmunoParserAction.COPY.toString(), name:"rawdata1",nodeType:"down",
            //                                    target:new TargetDef(cellRange:"a2:c2",sheetNum:0, sheetName:"Raw_Data", name:"RawDataLane", template:testRawDataTemplate))])
            //                    } 
            //                    // create the TemplateToParserDef instance
            //                   
            //                    templateToParserDefInstance=new TemplateToParserDef(template:templateInstance, parserDefs:[setupToLanesParserDefInstance, rawDataToGelInspectorDefInstance, rawTextDataToGelInspectorDefInstance] ).save(failOnError: true);
            //                    
            //                    
            //                    log.info "templateToParserDef $TemplateToParserDef.count";
            //                    file.delete()   
            //                    session.removeAttribute("openFileName")
            //                    session.removeAttribute("xml")
            //                    session.removeAttribute("file")
            //                    render(text: """<script type="text/javascript"> alert("$fileName saved !"); </script>""", contentType: 'text/javascript')
            //                }else if(templateInstance && purpose=="gelInspector"){
            //                                    
            //                    file.delete()   
            //                    session.removeAttribute("openFileName")
            //                    session.removeAttribute("xml")
            //                    session.removeAttribute("file")
            //                    session.removeAttribute("templateType")
            //                    render(text: """<script type="text/javascript"> alert("$fileName saved !"); </script>""", contentType: 'text/javascript')
            //                }
            //            }catch(Exception e){
            //                //                templateInstance.knowledgeList.clear()           
            //                deleteTemplate(templateInstance)          
            //                log.error e.getMessage()
            //                log.info "problem happens during save."
            //                file.delete() 
            //                session.removeAttribute("openFileName")
            //                session.removeAttribute("xml")
            //                session.removeAttribute("templateType")
            //                flash.message = "Template $fileName does not succesfully saved. Because ${e.getMessage()}. Please reopen the file."
            //                render(text: """<script type="text/javascript"> warningMessage("$flash.message") </script>""", contentType: 'text/javascript')
            //                render(template:"/ui/admin/warningMessage")
            //            }
            //     
        }else{
            flash.message = "You need to open the sheet again."
            render(text: """<script type="text/javascript"> warningMessage("$flash.message") </script>""", contentType: 'text/javascript')
            render(template:"/ui/admin/warningMessage") 
        }
 
    }
    
    //    def openExistTemplate={
    //        log.info "openExistSheet"
    //        log.info "choose template id ${params.id}"
    //        def templateInstance=Template.get(params.id)
    //        if( templateInstance){
    //            log.info "update "+ templateInstance.templateName
    //            session.openFileName=templateInstance.templateName
    //                         
    //            def excel=new excelReader(templateInstance.binaryFileData)
    //            def xmlData=excel.excelXmlBuilder()
    //           
    //            session.xml=xmlData
    //            session.templateType="old"
    //         
    //        }else{
    //           render(text: """<script type="text/javascript"> warningMessage("Can not find Template with ID ${params.id}") </script>""", contentType: 'text/javascript')
    //        }
    //
    //          
    //           render(text: """<script type="text/javascript"> redirect() </script>""", contentType: 'text/javascript')
    //    
    //    }
    //   
    def deleteDef(ParserDef pD){
        log.info "dirty parserDef $pD.name"
        def allConfigs=[]
        allConfigs+= pD.parserConfigurations
        log.info "parserConfigs count for parserDef $pD.id is ${allConfigs.size()}"
        allConfigs.each{conf->
            if(conf){                   
                def allSources=[]
                allSources+=conf.sources  
                log.info "sourceDef count for parserConfigs $conf.id is ${allSources.size()}"
                allSources.each{s->
                    if(s){
                                          
                        conf.removeFromSources(s)  
                        s.delete(flush: true)
                        log.info "sourceDef $s.id delete"
                    }          
                }
                       
                pD.removeFromParserConfigurations(conf)
                                
                def targets=TargetDef.findAllByParserConfiguration(conf)
                targets.each{ta->
                         
                    if(ta.delete())
                    {log.info "targetDef $ta.id delete"}
                }
                                  
                conf.delete(flush: true)
                log.info "parserConfig $conf.id delete"
            }                     
        }
        log.info "parserDef $pD.id delete"
        pD.delete()
 
    }
    def deleteTemplateToDefs(Template templateInstance){
        log.info "dirty templateToDefs $templateInstance.templateName"
//        def templateParserInstance= TemplateToParserDef.findByTemplate(templateInstance)             
//        if(templateParserInstance){                         
//            def allParserDefs = []
//            allParserDefs+= templateParserInstance.parserDefs
//            log.info "parserDefs count ${allParserDefs.size()}"
//            allParserDefs.each{pD->
//                
//                if(pD){
//                    templateParserInstance.removeFromParserDefs(pD)
//                    deleteDef(pD)                 
//                }
//
//              
//            }
//               
//         
//            templateParserInstance.delete()  
//            log.info "templateParser $templateParserInstance.id delete"
//        }
    }
    def deleteTemplate(Template templateInstance){
        def k=[]
        k+=templateInstance.knowledgeList
        k.each{knowledge->
            templateInstance.removeFromKnowledgeList(knowledge)
            knowledge.delete()            
        }

        //deleteTemplateToDefs(templateInstance)                      
        templateInstance.delete(flush: true)
    }

    def openSheet={
        log.info "openSheet" 
        session.putAt("username",springSecurityService.authentication.name)
        log.info "user ${session.username}"
        def webRootDir = servletContext.getRealPath("/")
        def savePath = webRootDir+"raw/"
        def dir = new File(savePath)
        if (!dir.exists()) {
            // attempt to create the path
            try {
                dir.mkdirs()
            } catch (Exception e) {
                response.setStatus(500, "could not create upload path ${savePath}")
                //render([written: false, fileName: file.name] as JSON)
                return false
            }
        }
        def f = request.getFile('myFile')
        def filePath=savePath
        def fileOrigName=f.getOriginalFilename()
        if(Template.findByTemplateName(fileOrigName)){
            flash.message = 'We already has template with the same name. If you are sure they are different template, please make an unique name and try again.' 
        }else{
        
            if(!f.empty) {
                if(!fileOrigName.endsWith("xls")){
                    flash.message = 'Sorry, currently we only accept .xls template.'   
                }else{
                    def existFiles=Template.findAllByTemplateName(fileOrigName)
                    log.info "${existFiles.size()}"
                    if(existFiles.size()>0){
                        log.info "same name template exists"
                        flash.message = 'Template with the same name already exists. Please change the name and re-open it.' 
                    }else{
                        
                        log.info "upload "+fileOrigName
                        session.openFileName=fileOrigName
                        filePath=filePath+"/"+System.nanoTime().toString()+"$fileOrigName"
                        File newFile=new File(filePath)
                        f.transferTo(newFile)
                        session.putAt("file", newFile)
                        // new excelReader(filePath ).eachLine{println "First column on row ${it.rowNum} = ${cell(0)}"}
                        def excel=new excelReader(filePath )
                        def xmlData=excel.excelXmlBuilder()
                        def dirtyKnowledgeList=Knowledge.findAllByFileName(fileOrigName)
                        if(dirtyKnowledgeList){
                            log.info"dirty knowledgeList for templateName $fileOrigName"
                            dirtyKnowledgeList.each{k->
                                k.delete()                        
                            }
                        }
                        // def xmlData=excel.excelXmlStringBuilderToDo()
                        session.filepath=filePath
                        session.xml=xmlData
                        session.templateType="new"
                    } 
                }        
            }
            else {
                flash.message = 'file cannot be empty'
            }
        }
        redirect(uri: "/admin")

    }
    
    def downloadTutorial={
        def webRootDir = servletContext.getRealPath("/")
        def tutorialPath= webRootDir+"Tutorial.pdf"
        File tutorial=new File(tutorialPath)
        if(tutorial){           
            response.setContentType("application/application/zip") 
            response.setHeader("Content-disposition", "attachment;filename=Tutorial.pdf")
            response.outputStream <<  tutorial.bytes
        }

    }
    
    def createParserConfig(){
        def templateList=Template.list()
        println templateList
        render(view: "/ui/admin/targetTemplate", model:[templateList:templateList])
    }

}
