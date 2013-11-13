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

import org.springframework.dao.DataIntegrityViolationException
import grails.converters.*
import org.hits.parser.*
import org.hits.ui.FileNameValidator
import org.hits.ui.Template
import org.hits.ui.VisualEntry
import java.text.SimpleDateFormat
import java.util.TimeZone;
import java.util.zip.ZipOutputStream  
import java.util.zip.ZipEntry  
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import grails.validation.ValidationException
import org.hits.ui.rdf.RdfBuilder
import org.hits.ui.labbook.LabBookSnippetBuilder



class ExperimentController {
    def springSecurityService
    def parserService 
    def auxillarySpreadsheetService
    def experimentParsersConfigService
    def performedExperimentParsersConfigService
    def mailService
    def formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
    def formatter2=new SimpleDateFormat("MMM dd yyyy HH:mm:ss", Locale.GERMANY);

    
    
    static final int BUFF_SIZE = 100000;

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
      
    }

  

    def deleteExperiment={
        try{
            def user = User.get(springSecurityService.principal.id)
        
            log.info "delete ${params.id} experiment"
     
            def experimentInstance = Experiment.get(params.id)  
            def experimentType=experimentInstance.type
            def experimentName=experimentInstance.filename
        
            if (!experimentInstance) {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])

            }
            if(checkUser(experimentInstance.author )){
                try {
            
                    experimentParsersConfigService.delete(experimentInstance)
                    
                    def r = []
                    r += experimentInstance.resources
                    r.each{resource->
                        experimentInstance.removeFromResources(resource)
                        resource.delete()        
                    }
                    def s=[]
                    s += experimentInstance.stages
                    s.each{stage->
                        experimentInstance.removeFromStages(stage)
                        stage.delete()        
                    }
                    def u=[]
                    u+=experimentInstance.updates
                    u.each{update->
                        experimentInstance.removeFromUpdates(update)
                        update.delete()            
                    }
                    experimentInstance.delete(flush: true)
                    
                    //delete rdf

                    def webRootDir = servletContext.getRealPath("/")
                    def savePath = webRootDir+"rdf/"
                    File rdfFile= new File(savePath+"${experimentInstance.id}.${user.id}.rdf")
                    if(rdfFile.exists()){
                        rdfFile.delete()
                        println "corresponding rdf deleted"
                    }
                    
                    //delete rdf
                    
                    
                    render(text: """<script type="text/javascript"> alert("$experimentName  and its rawdata files all deleted!"); </script>""", contentType: 'text/javascript')
             
                }
                catch (DataIntegrityViolationException e) {
                    flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])
                    redirect(uri:"/lab")
                }
            }else{
                render(text: """<script type="text/javascript"> warningMessage('Only the author can do this operation!'); </script>""", contentType: 'text/javascript')
    
            }
            if(experimentType && experimentType=="performed"){

                render(template:"/ui/user/performedexperiment", model:[experimentPInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='performed'}] )   
         
            }else{

                render(template:"/ui/user/experiment", model:[experimentInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='new'}] )    
      
            }
        }catch(MissingPropertyException ex){
            render(text: """<script type="text/javascript"> warningMessage('Your session might expired, try to log in again!'); </script>""", contentType: 'text/javascript')

        }
    }
    
    def mailToSeek(){
        println "sending start"
        def user = User.get(springSecurityService.principal.id)
        def allbinary
        def resourcesList
        try{
       
            def experimentInstance = Experiment.get(params.id)
            resourcesList=experimentInstance.resources.findAll{((it.state=="active") && (it.fileName.tokenize(".").last().toLowerCase()=='xls'||it.fileName.tokenize(".").last().toLowerCase()=='xlsx'))}
            println "resource size ${resourcesList.size()}"
            allbinary=auxillarySpreadsheetService.collateDataFiles(resourcesList,"")
        
            def youremailaccount=params.email
          
            log.info "send ${params.id} to $youremailaccount"
        
            def filename="${experimentInstance.filename.trim().replaceAll("\\s+", "_")}_all.xls"
            //            File f=new File("/tmp/$filename")
            //            f.setBytes(allbinary)
            mailService.sendMail {
                multipart true
                from "noreply-virtualliver@dkfz-heidelberg.de"  //for dkfz 
                to "${params.email}" 
                cc "seek@virtual-liver.de"
                subject "${experimentInstance.filename}"
                body "from Excemplify User: ${springSecurityService.authentication.name}"
                attachBytes filename,'application/vnd.ms-excel', allbinary
            }
                
            
            def sheetUpdate = new SheetUpdate(entityName:"ExperimentSentToVLN", state:"vln", comment:"setup files and gelinspector files for performed experiment ${params.experimentName} uploaded to SEEK(VLN)", dateUpdated: new Date())
            experimentInstance.addToUpdates(sheetUpdate)
        
            render(text: """<script type="text/javascript">alert("file uploaded to SEEK via Exemplify Tool User and To Yourself. Please Varify By Checking Your Email");</script>""", contentType: 'text/javascript') 
        }catch(Exception e){
            
            render(text: """<script type="text/javascript"> warningMessage('Exception occurs. ${e.getMessage()}'); </script>""", contentType: 'text/javascript')
          
        }

    } 
    
    
    def shareExperiment={
        def user = User.get(springSecurityService.principal.id)
      
        def experimentInstance = Experiment.get(params.id)
        def experimentType=experimentInstance.type
        def experimentName=experimentInstance.filename
        if(experimentInstance && checkUser(experimentInstance.author )){
            experimentInstance.share="public"
            if (!experimentInstance.save(flush: true)) {
                flash.message="Share operation is not succesful!"
                redirect(uri:"/lab")
 
            }
            def expUpdate = new SheetUpdate(comment:"Experiment is shared to public",dateUpdated:new Date()) //not sure if these are actually used
            experimentInstance.addToUpdates(expUpdate) 
            experimentInstance.save(flush:true) 
        
            render(text: """<script type="text/javascript"> alert("$experimentName is public experiment now!"); </script>""", contentType: 'text/javascript')
        }else{
            render(text: """<script type="text/javascript"> warningMessage('Only the author can do this operation!'); </script>""", contentType: 'text/javascript')
        
        }
        if(experimentType && experimentType=="performed"){

            render(template:"/ui/user/performedexperiment", model:[experimentPInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='performed'}] )   
         
        }else{
            render(template:"/ui/user/experiment", model:[experimentInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='new'}] )    
      
        }
          
      
    }
    
    def stopShareExperiment={
        def user = User.get(springSecurityService.principal.id)

        def experimentInstance = Experiment.get(params.id)
        def experimentName=experimentInstance.filename
        def experimentType=experimentInstance.type
        if(experimentInstance && checkUser(experimentInstance.author )){
            experimentInstance.share="private"
            if (!experimentInstance.save(flush: true)) {
                flash.message="Stop sharing operation is not succesful!"
                redirect(uri:"/lab")
 
            }
            def expUpdate = new SheetUpdate(comment:"Experiment is change to private",dateUpdated:new Date()) //not sure if these are actually used
            experimentInstance.addToUpdates(expUpdate) 
            experimentInstance.save(flush:true) 
            render(text: """<script type="text/javascript"> alert("$experimentName is private experiment now!"); </script>""", contentType: 'text/javascript')
        }else{
            render(text: """<script type="text/javascript"> warningMessage('Only the author can do this operation!'); </script>""", contentType: 'text/javascript')
           
        }
        if(experimentType && experimentType=="performed"){

            render(template:"/ui/user/performedexperiment", model:[experimentPInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='performed'}] )   
         
        }else{

            render(template:"/ui/user/experiment", model:[experimentInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='new'}] )    
      
        }
    }

    def checkUser={author->
        return (author==User.get(springSecurityService.principal.id))
    }

    def downloadlog={
        def   experimentInstance = Experiment.get(params.id)
        def logfile = writeUpdates(experimentInstance.updates)
        
        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "attachment;filename=${experimentInstance.filename.trim().replaceAll("\\s+", "_")}.log")
        response.outputStream << logfile.bytes     
        logfile.delete()
    }

    def visualizelog={
        def   experimentInstance = Experiment.get(params.id)
        log.info "visual"
        Map visualEntries=[:]
        experimentInstance.updates.each{update->
             
            if(update.entityName && update.state && update.fileNameVersion){
                if(visualEntries.containsKey(update.fileNameVersion)){
                    VisualEntry currentVisEntry=visualEntries.get(update.fileNameVersion)
                    currentVisEntry.stateMap.put(update.dateUpdated, update.state)
                     
                }else {
                    VisualEntry visEntry=new VisualEntry() 
                    visEntry.entityName=update.entityName
                    visEntry.fileNameVersion=update.fileNameVersion
                    Map states=[:]
                    states.put(update.dateUpdated, update.state)
                    visEntry.stateMap=states
                    visualEntries.put(update.fileNameVersion, visEntry)
                }
     
            }
             
        }
         
        /**write jsonString
         */
        
  
        def jsonString="["
        int count=0
       
        visualEntries.each{k,v->
            
            count++
            jsonString=jsonString+"{"
            jsonString=jsonString+"\"name\":\"${v.entityName}\", \"blot\":\"${v.entityName.tokenize("_").first()=="rawdata"?v.entityName.tokenize("_").last():"notrawdata"}\", \"file\":\"${v.fileNameVersion}\",\"states\":["
            int count2=0
            v.stateMap.each{kstate,vstate->
                count2++  

                jsonString=jsonString+"[\"${formatter.format(kstate)}\",\"${vstate}\"]"
                if(count2<v.stateMap.size()){
                    jsonString=jsonString+","  
                }
                
            }
            jsonString=jsonString+"], \"times\":["
            int count3=0
            v.stateMap.each{kstate,vstate->
                count3++  
                jsonString=jsonString+"[\"${formatter.format(kstate)}\",\"${formatter.format(kstate)}\"]"
                if(count3<v.stateMap.size()){
                    jsonString=jsonString+","  
                }
                
            }
            
            jsonString=jsonString+"]"
            jsonString=jsonString+"}"
            if(count<visualEntries.size()){
                jsonString=jsonString+","
            }
          
            
        }
        jsonString=jsonString+"]"
       
        //                    
        println jsonString 
        
        session.putAt("experimentLog",jsonString)
        redirect(uri:"/lab/logview?experimentId="+params.id)
    }

    def writeUpdates(updates){
        def webRootDir = servletContext.getRealPath("/")
        def updateFile = File.createTempFile("updates",".log")
        updates.each{
            updateFile << "${it.dateUpdated} ${it.comment} \n"
        }
        return updateFile
    }

    
    def downloadAllZip={
        File zipfile
        try{
            def experimentInstance = Experiment.get(params.id)
            //def user=User.get(springSecurityService.principal.id)
           def user=experimentInstance.author
            zipfile= auxillarySpreadsheetService.createExperimentZip(experimentInstance, user)
            response.setContentType("application/application/zip") 
            response.setHeader("Content-disposition", "attachment;filename=${zipfile.getName()}")
            response.outputStream <<  zipfile.bytes
        }catch(Exception e){
            flash.message = e.getMessage()
            redirect(uri:"/exception")
        }
        zipfile.delete()
    }
    //    
    //    def downloadAll={
    //        def   experimentInstance = Experiment.get(params.id)
    //        def allbinary
    //        def resourcesList=experimentInstance.resources.findAll{it.state=="active"}
    //        println "resource size ${resourcesList.size()}"
    //        try{
    //            allbinary=auxillarySpreadsheetService.collateDataFiles(resourcesList,"")
    // 
    //            if (experimentInstance) {
    //                try {
    //
    //                    response.setContentType("application/vnd.ms-excel")
    //                    response.setHeader("Content-disposition", "attachment;filename=${experimentInstance.filename.trim().replaceAll("\\s+", "_")}_all.xls")
    //                    response.outputStream << allbinary
    //
    //                }
    //                catch(Exception ex){
    //                    flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])}"
    //      
    //                }
    //            }
    //            else{
    //                flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])}"
    //         
    //            }
    //        }catch(ValidationException e){
    //            e.errors.each {
    //                println it
    //                flash.message+="${it.toString()}"
    //            }
    //          
    //        
    //        }   
    //    }
    //    def downloadPerform={
    //        def   experimentInstance = Experiment.get(params.id)
    //        def resourcesList=experimentInstance.resources.findAll{(it.type=="gelinspector" || it.type=="setup")&&(it.state=="active")&& (it.fileName.tokenize(".").last().toLowerCase()=='xls'||it.fileName.tokenize(".").last().toLowerCase()=='xlsx')}
    //        println "resource size ${resourcesList.size()}"
    //        try{
    //            experimentInstance.binaryData=auxillarySpreadsheetService.collateDataFiles(resourcesList,"")
    //            experimentInstance.save(failOnError: true)
    //            if ( experimentInstance) {
    //                try {
    //
    //                    response.setContentType("application/vnd.ms-excel")
    //                    response.setHeader("Content-disposition", "attachment;filename=${experimentInstance.filename.trim().replaceAll("\\s+", "_")}_workbook.xls")
    //                    response.outputStream <<  experimentInstance.binaryData
    //
    //             
    //
    //                }
    //                catch(Exception ex){
    //                    flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])}"
    //                    redirect(action: "list")
    //                }
    //            }
    //            else{
    //                flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])}"
    //                redirect(action: "list")
    //            }
    //        }catch(ValidationException e){
    //            e.errors.each {
    //                println it
    //                flash.message+="${it.toString()}"
    //            }
    //          
    //            redirect(action: "list")
    //        }
    //        //        Logger logger=Logger.getLogger("${experimentInstance.filename}")
    //        //        logger.info("workbook downloaded once"); 
    //    }
    
    def download={
        def   experimentInstance = Experiment.get(params.id)   

        if ( experimentInstance) {
            try {
                if(experimentInstance.contentType=="xlsx"){
                    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        
                    response.setHeader("Content-disposition", "attachment;filename=${experimentInstance.filename.trim().replaceAll("\\s+", "_")}_workbook.xlsx")
             
                }else{
                            
                    response.setContentType("application/vnd.ms-excel")
                    response.setHeader("Content-disposition", "attachment;filename=${experimentInstance.filename.trim().replaceAll("\\s+", "_")}_workbook.xls")
             
                }
               
                response.outputStream <<  experimentInstance.binaryData

             

            }
            catch(Exception ex){
                flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])}"
                redirect(action: "list")
            }
        }
        else{
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])}"
            redirect(action: "list")
        }
     
    }
    def generateExport={  //autogeneration half prepared gelinspector file for performed data
        def experimentInstance = Experiment.get(params.id)
        def user=User.get(springSecurityService.principal.id)
        def rawdataList = experimentInstance.resources.findAll{ it.type=="rawdata" && it.state=="active"}
        def setUpFileTemplate = Template.findByTemplateName(experimentInstance.setUpTemplateName)
        File zipfile 
        // def rawDataTemplate = Template.findByTemplateName("rawdata_template.xls") //currently we have only 1 template for raw data exporting
        println "num raw data ${rawdataList.size()}"
        if (params.layout=="other"){
            flash.message="Other layouts not implemented yet"
            redirect(uri:"/lab")
            return
        }
      
        def parserType = parserService.getParserName(params.layout)//this would change to params.layout
        //
        if(!ExperimentToParserDef.findByExperiment(experimentInstance)){
            performedExperimentParsersConfigService.defaultConfig(experimentInstance)
        }
        //
        
        
        def newWorkbook=experimentInstance.binaryData
        def filenamesstring=""
        try{
            rawdataList.each{ resource ->
                session.setAttribute("ExperimentWorkbook",newWorkbook)//required
                session.putAt("fileName",resource.fileName)//required
            
                def datafile = File.createTempFile("${System.nanoTime()}",resource.fileName)
                //test if file is a text or an excel file
                println resource.fileName
                if (resource.fileName =~ /.txt$/){
                    println "textfile raw data file,skip it"
               
                }
                else{
                    def expRUpdate = new SheetUpdate(entityName:"rawdata_${resource.fileName.tokenize(".").first()}",fileNameVersion:"${resource.fileName}[version:${resource.fileversion?formatter.format(resource.fileversion):"old"}]", state:"export", comment:"Rawdata file: ${resource.fileName} is exported into GelInspector Layout.",dateUpdated:new Date()) //not sure if these are actually used
                    experimentInstance.addToUpdates(expRUpdate) 
                
                    datafile.setBytes(resource.binaryData)
                    //actually, we need the           
                    parserService.parseSpreadsheet(datafile,[experiment:experimentInstance,parserType:parserType])   //parserType ->parserDefType
                    newWorkbook = session.getAttribute("parsedFile")
               
                    session.removeAttribute("parsedFile")
                    // currentExperimentInstance.addToUpdates(expUpdate) //now we don't want to update the workbook each time we export
                  
                    filenamesstring="$filenamesstring ${resource.fileName} "
                }
                datafile.delete()
            }
            if(checkUser(experimentInstance.author)){
             
                def expUpdate = new SheetUpdate(comment:"Exported workbook and rawdata files: ${filenamesstring}",dateUpdated:new Date()) //not sure if these are actually used
                experimentInstance.addToUpdates(expUpdate) 
                //experimentInstance.binaryData=newWorkbook
                experimentInstance.save(flush:true) 
            }else{
                println "it is not the author" 
            }
       
            //use auxillarySpreadsheetService to split the Gel Inspector files into 3 separate spreadsheets and return a Zip of them
            zipfile= auxillarySpreadsheetService.createGelInspectorZip(experimentInstance, user, newWorkbook)
               
            response.setContentType("application/application/zip") 
            response.setHeader("Content-disposition", "attachment;filename=${zipfile.getName()}")
            response.outputStream <<  zipfile.bytes
           
        }catch(Exception e){
            flash.message = e.getMessage()
            redirect(uri:"/exception")
        }
        zipfile.delete()
    }
    
    
    def downloadOrigExport={  //original gelinspector files collection for performed data
        def experimentInstance = Experiment.get(params.id)
        def type=params.type

        if ( experimentInstance) {
            
            def dataResources=experimentInstance.resources.findAll{(it.type=="$type")&&(it.state=="active")}
            if(dataResources.size()>0){
            
                String zipFileName =File.createTempFile("Orig_$type",".zip").toString(); 

                ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(zipFileName))  
                dataResources.each{ gelother-> 
            
                    zipFile.putNextEntry(new ZipEntry("${gelother.fileName}"))  
           
                    zipFile.write(gelother.binaryData)
                
                 
                    zipFile.closeEntry()  
                }
                zipFile.close()  
                def zipfile=new File(zipFileName)
                response.setContentType("application/application/zip") 
                response.setHeader("Content-disposition", "attachment;filename=${zipfile.getName()}")
                response.outputStream <<  zipfile.bytes
                zipfile.delete();
            }else{
                flash.message = "less than 1 active data sources"
                redirect(uri:"/exception")
            }
        }else{
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'experiment.label', default: 'Experiment'), params.id])}"
            redirect(uri:"/exception")
        }
        
    }
    def downloadSnippet={
        def experimentInstance = Experiment.get(params.id)
        try{
            LabBookSnippetBuilder snippet=new LabBookSnippetBuilder(experimentInstance)
            response.setContentType("application/text/html") 
            response.setHeader("Content-disposition", "attachment;filename=${experimentInstance.filename}_labbook_snippet.html")
            byte[] snippetbytes=snippet.generateSnippet()
            println snippetbytes.size()
            response.outputStream << snippetbytes
        }catch(Exception e){
            flash.message = e.getMessage()
            redirect(uri:"/exception")
        }
    }   
    def downloadRdf={       
        def experimentInstance = Experiment.get(params.id)
        def webRootDir = servletContext.getRealPath("/")
        def user=experimentInstance.author
        def savePath = webRootDir+"rdf/"
     
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
        File rdfFile= new File(savePath+"${experimentInstance.id}.${user.id}.rdf")
        if(rdfFile.exists()){
            response.setContentType("application/application/rdf+xml") 
            response.setHeader("Content-disposition", "attachment;filename=${rdfFile.getName()}")
            response.outputStream << rdfFile.bytes       
        }else{
            def setUpFileTemplate = Template.findByTemplateName(experimentInstance.setUpTemplateName)
            RdfBuilder builder=new RdfBuilder(experimentInstance)
  
            if (params.layout=="rdf"){
                println "we are trying to export your excels into rdf to make it searchable"
       
         
       
                builder.exportWorkbookIntoRdf(rdfFile)  

                response.setContentType("application/application/rdf+xml") 
                response.setHeader("Content-disposition", "attachment;filename=${rdfFile.getName()}")
                response.outputStream << rdfFile.bytes         
            
            
            }
        }
    
       
   
  
    }
   

    def exportRdf={     
        def experimentInstance = Experiment.get(params.id)
        def user=User.get(springSecurityService.principal.id)
        def webRootDir = servletContext.getRealPath("/")
        def savePath = webRootDir+"rdf/"
     
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
        File rdfFile= new File(savePath+"${experimentInstance.id}.${user.id}.rdf")      
        def setUpFileTemplate = Template.findByTemplateName(experimentInstance.setUpTemplateName)
        RdfBuilder builder=new RdfBuilder(experimentInstance)
  
        if (params.layout=="rdf"){
            println "we are trying to export your excels into rdf to make it searchable"
   
            builder.exportWorkbookIntoRdf(rdfFile)  

            response.setContentType("application/application/rdf+xml") 
            response.setHeader("Content-disposition", "attachment;filename=${rdfFile.getName()}")
            response.outputStream << rdfFile.bytes         
            
            
        }else{
            redirect(uri:"/lab")
            return  
        }
     
    }


    def exportInto={  //autogenerate gelinspector file for new experiment
        def experimentInstance = Experiment.get(params.id)
        def user=User.get(springSecurityService.principal.id)
        def rawdataList = experimentInstance.resources.findAll{ it.type=="rawdata" && it.state=="active"}
        def setUpFileTemplate = Template.findByTemplateName(experimentInstance.setUpTemplateName)
        // def rawDataTemplate = Template.findByTemplateName("rawdata_template.xls") //currently we have only 1 template for raw data exporting
        println "num raw data ${rawdataList.size()}"
        if (params.layout=="other"){
            flash.message="Other layouts not implemented yet"
            redirect(uri:"/lab")
            return
        }
        def parserType = parserService.getParserName(params.layout)//this would change to params.layout
        //
        if(!ExperimentToParserDef.findByExperiment(experimentInstance)){
            
        }
        //
        
        
        def newWorkbook=experimentInstance.binaryData
        def filenamesstring=""
        try{
            rawdataList.each{ resource ->
                session.setAttribute("ExperimentWorkbook",newWorkbook)//required
                session.putAt("fileName",resource.fileName)//required
            
                def datafile = File.createTempFile("${System.nanoTime()}",resource.fileName)
                //test if file is a text or an excel file
                println resource.fileName
                if (resource.fileName =~ /.txt$/){
                    println "textfile raw data file,skip it"
               
                }
                else{
                    def expRUpdate = new SheetUpdate(entityName:"rawdata_${resource.fileName.tokenize(".").first()}",fileNameVersion:"${resource.fileName}[version:${resource.fileversion?formatter.format(resource.fileversion):"old"}]", state:"export", comment:"Rawdata file: ${resource.fileName} is exported into GelInspector Layout.",dateUpdated:new Date()) //not sure if these are actually used
                    experimentInstance.addToUpdates(expRUpdate) 
                
                    datafile.setBytes(resource.binaryData)
                    //actually, we need the           
                    parserService.parseSpreadsheet(datafile,[experiment:experimentInstance,parserType:parserType])   //parserType ->parserDefType
                    newWorkbook = session.getAttribute("parsedFile")
               
                    session.removeAttribute("parsedFile")
                    // currentExperimentInstance.addToUpdates(expUpdate) //now we don't want to update the workbook each time we export
                  
                    filenamesstring="$filenamesstring ${resource.fileName} "
                }
                datafile.delete()
            }
            if(checkUser(experimentInstance.author)){
                println "start check user" 
                def expUpdate = new SheetUpdate(comment:"Exported workbook and rawdata files: ${filenamesstring}",dateUpdated:new Date()) //not sure if these are actually used
                experimentInstance.addToUpdates(expUpdate) 
                //experimentInstance.binaryData=newWorkbook
                experimentInstance.save(flush:true) 
            }else{
                println "it is not the author" 
            }
       
            //use auxillarySpreadsheetService to split the Gel Inspector files into 3 separate spreadsheets and return a Zip of them
            File zipfile = auxillarySpreadsheetService.createGelInspectorZip(experimentInstance, user, newWorkbook)     
            println "start give response"  
            response.setContentType("application/application/zip") 
            response.setHeader("Content-disposition", "attachment;filename=${zipfile.getName()}")
            response.outputStream <<  zipfile.bytes
            zipfile.delete()
            return null
        }catch(Exception e){
            flash.message = e.getMessage()
            println (e.getMessage())
            redirect(uri:"/exception")
        }

    }
    

    def finishUpload={
        def expType=params.exptype
        def resType=params.restype
        def section
        println "expType $expType  resType $resType"
        switch (expType){
        case 'new': section="0"
            break
        case 'old': section="1"
            break
        default:    section="0"
        }
        def duplicateList=[]
        def resourcesList=[]
        def user=User.get(springSecurityService.principal.id)
        def expId=params.id
        log.info "data update to $expId"
        def currentExperimentInstance=Experiment.get(expId); 
//        if(resType=="rawdata"){
            try{
                def rawdataList=[]   
                def gelList
                if(expType=="new"){
                    resourcesList=currentExperimentInstance.resources.findAll{ it.type=="rawdata" || it.type=="setup"||it.type=="other"}     
              
                }else if(expType=="old"){
                    resourcesList=currentExperimentInstance.resources.findAll{it.type=="setup" ||it.type=="gelinspector"||it.type=="rawdata"||it.type=="other"}
              
                }
               
                //    def rawTextDataTemplate = Template.findByTemplateName("rawTextDataTemplate")
    
                session.rawdata.each(){e->
                    def convertedFile
                    def convertedFileName
            
                    /**first found same name (name before .) file in the experiment
                     */
                    println "$resType $e"
                    def resourceDuplicateHitResource=currentExperimentInstance.resources.find{ it.fileName.tokenize(".").first() ==e.fileName.tokenize(".").first() && it.state=="active"}        
                    if(resourceDuplicateHitResource){
                        resourceDuplicateHitResource.state="inactive"
                        resourceDuplicateHitResource.save(flush:true)
                        duplicateList.add(e.fileName) 
                        SheetUpdate expUpdateduplicate = new SheetUpdate(entityName:"${resType}_${e.fileName.tokenize(".").first()}",fileNameVersion:"${resourceDuplicateHitResource.fileName}[version:${resourceDuplicateHitResource.fileversion?formatter.format(resourceDuplicateHitResource.fileversion):"old"}]", state:"deactive", dateUpdated:new Date(), comment:"because of new ${e.fileName}, old version of ${resourceDuplicateHitResource.fileName} are automatically deactived")
                        currentExperimentInstance.addToUpdates(expUpdateduplicate)
                        log.info "${e.fileName} duplicated"
                    }
           
            
                    /**
                     *convert txt file into xls
                     */ 
            
            
                    if ((e.fileName =~ /.txt$/)&&(resType=="rawdata")){
                        session.putAt("ExperimentWorkbook",currentExperimentInstance.binaryData)
                        session.putAt("fileName",e.fileName)
                        println "this is a text file, lets transform it to excel!"
                        parserService.parseSpreadsheet(e.binaryData,[experiment:currentExperimentInstance,parserType:"Raw Data (Text) to Excel"])
                        convertedFileName=e.fileName.replace(".txt",".xls")
                        convertedFile=session.getAttribute("parsedFile")
                        e.state="inactive"
                        session.removeAttribute("parsedFile")
                        session.removeAttribute("parsedFileName")
                        println "updateTxtConvertInfo"
                        SheetUpdate txtSheetUpdate = new SheetUpdate(entityName:"${resType}_${e.fileName.tokenize(".").first()}",fileNameVersion:"${e.fileName}[version:${e.fileversion?formatter.format(e.fileversion):"old"}]", state:"converted to xls", dateUpdated:new Date(), comment:"${e.fileName} is converted to ${convertedFileName},  original ${e.fileName} are automatically deactived")
                        currentExperimentInstance.addToUpdates(txtSheetUpdate)
                    }
 
                    /**
                     *put in here transforming the text files to new style raw data files
                     */
         
                    resourcesList.add(e)
                    rawdataList.add(e)
                    if (convertedFile!=null){
                        def conversion = new Resource(fileName:convertedFileName, type:"$resType", binaryData:convertedFile, author:user,state:"active", fileversion: new Date());
                        conversion.save(failOnError:true)
                        SheetUpdate convertSheetUpdate = new SheetUpdate(entityName:"${resType}_${convertedFileName.tokenize(".").first()}",fileNameVersion:"${convertedFileName}[version:${formatter.format(conversion.fileversion)}]", state:"add/create", dateUpdated:conversion.fileversion, comment:"${convertedFileName} is added to ${currentExperimentInstance.filename}")
                        currentExperimentInstance.addToUpdates(convertSheetUpdate)
                        resourcesList.add(conversion)
                        rawdataList.add(conversion)
                    }
                    println "resourcelistSize ${resourcesList.size()}"
                    SheetUpdate expUpdate = new SheetUpdate(entityName:"${resType}_${e.fileName.tokenize(".").first()}", fileNameVersion:"${e.fileName}[version:${formatter.format(e.fileversion)}]", state:"add/create", dateUpdated:new Date(), comment:"raw data file ${e.fileName} added to ${currentExperimentInstance.filename}")
                    currentExperimentInstance.addToUpdates(expUpdate)
   
                }
        
       
                log.info resourcesList.size();         

                currentExperimentInstance.resources=resourcesList
                currentExperimentInstance.save(failOnError:true,flush:true)
                log.info "raw data resource size ${currentExperimentInstance.resources.size()}"
           
                session.removeAttribute("rawdata")
                session.removeAttribute("rawdatamap")
                session.removeAttribute("experimentId")
                session.removeAttribute("info")
        
                if(duplicateList.size()>0){
                    String duplicate=""

                    duplicateList.each(){d->
                        duplicate=duplicate+d+" "
                    }
         
                    flash.message = "${currentExperimentInstance.filename} already containts rawdata files: $duplicate , old version are automatically deactived."
                }
            }catch(Exception e){
                session.rawdata.each{v ->
                    log.info v.id
                    v.delete()
                } 
                log.info(e.getMessage())
                session.removeAttribute("parsedFile")
                session.removeAttribute("parsedFileName")
                session.removeAttribute("experimentId")
                session.removeAttribute("info")
                session.removeAttribute("rawdata")
                session.removeAttribute("rawdatamap")  
            }

        session.putAt("active", "$section")
        redirect(uri:"/lab")
        //        render(text: """<script type="text/javascript"> goBack(); </script>""", contentType: 'text/javascript')
   

    }
    

    def deleteResourceDuringUpload={

        log.info "raw data size before ${session.rawdata?.size()}}"
        def fileName=params.deleteFileName
        log.info "delete $fileName"
        
        try{
            def deleteResource=session.rawdatamap.get(fileName)  
            session.rawdata.remove(deleteResource)
            session.rawdatamap.remove(fileName)
            log.info deleteResource.id
            deleteResource.delete()
            Thread.currentThread().sleep(100)
            log.info "raw data size after ${session.rawdata.size()}}"
            log.info "raw data map size after ${session.rawdatamap.size()}}"
        }catch(Exception e){
            log.info e.getMessage()
        }
    
     

    }
    
    def traditionalUploadAction={
        def expId=params.experimentId
        def expType=params.experimentType
        def resType=params.resourceType
        
        println params.experimentId
        def user=User.get(springSecurityService.principal.id)
        def f = request.getFile('myRawData')
        def webRootDir = servletContext.getRealPath("/")
        def savePath = webRootDir+"rawdata"
        def dir 		= new File(savePath)
        if(!dir.exists()){
            dir.mkdirs()
        }
        def filePath=savePath
        def fileName="null"
        def infoMessage="";
        def proved
        if(session.getAttribute("info")){
            infoMessage=session.getAttribute("info")
        }      
           
        fileName=f.getOriginalFilename()
        filePath=filePath+"/"+System.nanoTime().toString()+"${fileName}"
        
        if((!f.empty) && (resType=="rawdata")) {
            print "upload raw data "+fileName
            proved =FileNameValidator.validateRawDataFileName(fileName)
            if(proved){          
                File newFile=new File(filePath)
                f.transferTo(newFile)
                def  rawDataResource=new Resource(fileName:fileName, type:"$resType", binaryData: newFile.bytes, author:user,state:"active", fileversion: new Date());
                rawDataResource.save(failOnError: true);
                if(!session.rawdata){
                    println "initial "
                    def resourceList=[]
                    def resourceNameMap=[:]
                    resourceList.add(rawDataResource)
                    resourceNameMap.put(fileName, rawDataResource)
                       
                    session.rawdatamap=resourceNameMap
                    session.rawdata=resourceList
                 
                    Thread.currentThread().sleep(50)
                }else{
                    println "add"
                    if(!session.rawdatamap.containsKey(fileName)){
                        session.rawdata.add(rawDataResource)
                        session.rawdatamap.put(fileName, rawDataResource)
                    }else{
                        rawDataResource.delete()
                        log.info"duplicate"
                        flash.message="$fileName duplicate "
                        
                    } 
                    
                    Thread.currentThread().sleep(50)
                }
                newFile.delete()
                infoMessage=infoMessage+fileName+" "
                session.info= infoMessage
            }else{
                log.info "$fileName does not match the raw data name conventeion. "
                flash.message="$fileName does not match the raw data name conventeion. "
            }
            
        }else if((!f.empty) && (expType=="old") && (resType=="gelinspector")){
            print "upload gel inspector file "+fileName
            proved=FileNameValidator.validateGelDataFileName(fileName)
            if(proved){
                File newFile=new File(filePath)
                f.transferTo(newFile)
                def  gelDataResource=new Resource(fileName:fileName, type:"$resType", binaryData: newFile.bytes, author:user,state:"active", fileversion: new Date());
                gelDataResource.save(failOnError: true);
                if(!session.geldata){
                    println "initial gel"
                    def resourceList=[]
                    def resourceNameMap=[:]
                    resourceList.add(gelDataResource)
                    resourceNameMap.put(fileName, gelDataResource)
                       
                    session.geldatamap=resourceNameMap
                    session.geldata=resourceList
                 
                    Thread.currentThread().sleep(50)
                }else{
                    println "add gel"
                    if(!session.geldatamap.containsKey(fileName)){
                        session.geldata.add(gelDataResource)
                        session.geldatamap.put(fileName, gelDataResource)
                    }else{
                        gelDataResource.delete()
                        log.info"duplicate"
                        flash.message="$fileName duplicate "
                        
                    } 
                    
                    Thread.currentThread().sleep(50)
                }
                newFile.delete()
                infoMessage=infoMessage+fileName+" "
                session.info= infoMessage
            }else{
                log.info "$fileName does not match the raw data name conventeion. "
                flash.message="$fileName does not match the raw data name conventeion. "
            }
        
        }else if((!f.empty) && (resType=="other")){
            print "upload other "+filName
            fileName=f.getOriginalFilename()
            proved=FileNameValidator.validateOtherDataFileName(fileName)
            if(proved){
                File newFile=new File(filePath)
                f.transferTo(newFile)
                def  otherDataResource=new Resource(fileName:fileName, type:"$resType", binaryData: newFile.bytes, author:user,state:"active", fileversion: new Date());
                otherDataResource.save(failOnError: true);
                if(!session.geldata){
                    println "initial other"
                    def resourceList=[]
                    def resourceNameMap=[:]
                    resourceList.add(otherDataResource)
                    resourceNameMap.put(fileName, otherDataResource)                  
                    session.otherdatamap=resourceNameMap
                    session.otherdata=resourceList
                 
                    Thread.currentThread().sleep(50)
                }else{
                    println "add other"
                    if(!session.otherdatamap.containsKey(fileName)){
                        session.otherdata.add(gelDataResource)
                        session.otherdatamap.put(fileName, otherDataResource)
                    }else{
                        otherDataResource.delete()
                        log.info"duplicate"
                        flash.message="$fileName duplicate "
                        
                    } 
                    
                    Thread.currentThread().sleep(50)
                }
                newFile.delete()
                infoMessage=infoMessage+fileName+" "
                session.info= infoMessage
            }else{
                log.info "$fileName does not match the other name conventeion. "
                flash.message="$fileName does not match the other name conventeion. "
            }
        }
        else{
            log.info "empty file! or unsure experiment type !"
            flash.message="empty file, please check"
        }
          
       
        def urlString="/lab/uploadr?experimentType="+expType+"&&experimentId="+expId+"&&resourceType="+resType
        redirect(uri:urlString )
    }


    def uploadrAction={    
        def user=User.get(springSecurityService.principal.id)
        byte[] buffer = new byte[BUFF_SIZE];
        def contentType	= request.getHeader("Content-Type") as String
        def fileName    = request.getHeader('X-File-Name') as String
        def fileSize 	= request.getHeader('X-File-Size') as Long
        def name  = request.getHeader('X-Uploadr-Name') as String
        def info		= session.getAttribute('uploadr')
        //def savePath	= ((name && info && info.get(name) && info.get(name).path) ? info.get(name).path : "/tmp") as String
        def webRootDir = servletContext.getRealPath("/")
        def path=info.get(name).path
        def savePath = webRootDir+"rawdata"
        def dir 		= new File(savePath)
        if(!dir.exists()){
            dir.mkdirs()
        }
        log.info "fileName:$fileName ->type: $path-> savePath:$savePath -> fileSize:$fileSize"
      
        def  rawDataResource
        def resourceList
        Map resourceNameMap
        int status      = 0
        def statusText  = ""
        def proved=true
        if(path=="rawdata"){
            proved=FileNameValidator.validateRawDataFileName(fileName)  
        }     
        if(proved){
            
            File file    = File.createTempFile(System.nanoTime().toString(),fileName,dir)

            response.contentType    = 'application/json'
            InputStream inStream = null
            OutputStream outStream = null
            // handle file upload
            try {
                println "hello uploading"
                inStream = request.getInputStream()
                outStream = new FileOutputStream(file)

                // ByteArrayOutputStream baos = new ByteArrayOutputStream()

                while (true) {
                    synchronized (buffer) {
                        int amountRead = inStream.read(buffer);

                        if (amountRead == -1) {

                            break
                        }
                        outStream.write(buffer, 0, amountRead)

                    }

                }
                outStream.flush()
                status      = 200
                statusText  = "'${fileName}' upload successful!"
                   
                //            def currentExperimentInstance=Experiment.get(expId);
                rawDataResource=new Resource(fileName:fileName, type:"$path", binaryData:file.bytes, author:user,state:"active", fileversion: new Date());
                rawDataResource.save(failOnError: true);
           
                if(!session.rawdata){
                    println "initial "
                    resourceList=[]
                    resourceNameMap=[:]
                    resourceList.add(rawDataResource)
                    resourceNameMap.put(fileName, rawDataResource)
                    session.rawdatamap=resourceNameMap
                    session.rawdata=resourceList
             
                    Thread.currentThread().sleep(50)
                }else{
                    println "add"
                    if(!session.rawdatamap.containsKey(fileName)){
                        session.rawdata.add(rawDataResource)
                        session.rawdatamap.put(fileName, rawDataResource)
                    }else{
                        rawDataResource.delete()
                        println "duplicate"
                        status      = 500
                        statusText  = "duplicated"
                    } 
                
                    Thread.currentThread().sleep(50)
                }
            


            } catch (Exception e) {
                // whoops, looks like something went wrong
                log.error  e.getMessage()
    
                println "hello"
                status      = 500
                statusText  = e.getMessage()
                if(session.rawdatamap.containsKey(fileName)){
                    session.rawdata.remove(rawDataResource)
                    session.rawdatamap.remove(fileName)
                }
            } finally {
                if (inStream != null) inStream.close()
                if (outStream != null) outStream.close()
               
            } 
            
            // make sure the file was properly written
            if (status == 200) {
                // whoops, looks like the transfer was aborted!
            
                if(fileSize > file.size()){
                    status      = 500
                    //statusText  = "'${file.name}' transfer incomplete, received ${file.size()} of ${fileSize} bytes"
                    statusText  = "'${fileName}' transfer incomplete, received ${file.size()} of ${fileSize} bytes"}
            }else{
                if(session.rawdatamap.containsKey(fileName)){
                    session.rawdata.remove(rawDataResource)
                    session.rawdatamap.remove(fileName)
                }
            }
            
            
           
            file.delete()
        }else{
            log.info "'${fileName}' does not match the raw data name conventeion. "
            status      = 500
            //statusText  = "'${file.name}' transfer incomplete, received ${file.size()} of ${fileSize} bytes"
            statusText  = "'${fileName}' does not match the raw data name conventeion " 
        }
        

        flash.message="successfullly uploaded file havent sent to experiment yet"
 
        response.setStatus(status, statusText)
 
        render([written: (status == 200), fileName: fileName, status: status, statusText: statusText] as JSON)
 

    }

    def downloadRawData(){
        def experimentInstance =Experiment.get(params.id)
        
        def dataFilesList=experimentInstance.resources.findAll{ it.type=="rawdata" && it.state=="active" }
        
        def allDataWorkbookBA=auxillarySpreadsheetService.collateDataFiles(dataFilesList,"raw_data_")
        
        response.setContentType("application/vnd.ms-excel") 
        response.setHeader("Content-disposition", "attachment;filename=rawdata_${experimentInstance.filename.trim().replaceAll("\\s+", "_")}.xls")
        response.outputStream <<  allDataWorkbookBA

    }
    
    
    
}
