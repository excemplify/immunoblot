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
import org.hits.ui.*
import grails.converters.JSON
import org.hits.parser.*
import org.hits.ui.KnowledgeFetcher
import java.text.SimpleDateFormat
import org.hits.ui.exceptions.RuleViolateException


/**
 *
 * @author rongji
 */

class LabController {
    def springSecurityService
    def parserService
    def experimentParsersConfigService
    def performedExperimentParsersConfigService
    // def mailService
    def formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
    def auxillarySpreadsheetService
    
    static INNERRAWDATATEMPLATENAME="rawdata_template.xls"
    static INNERLOADINGTEMPLATENAME="laneloading_template_0723.xls"
    static INNERGELINSPECTORTEMPLATENAME="gelInspectorTemplate_0723.xls"

    def index() {
 
    }
 
    
    def updateExp(){
        log.info "update Exp"
        boolean reConfigParsers
        def user = User.get(springSecurityService.principal.id)
        session.putAt("username",springSecurityService.authentication.name)
        log.info "ExperimentId: ${params.experimentId}"
        log.info "ExperimentName: ${params.experimentName}"
        log.info "ExperimentTopic: ${params.experimentTopic}"
        log.info "Using setupTemplate: ${params.setuptemplate}"
        log.info "Using loadingTemplate: ${params.loadingtemplate}"
        log.info "Using rawdataTemplate: ${params.rawdatatemplate}"
        log.info "Using gelinspectorTemplate: ${params.gelinspectortemplate}"
        log.info "Set Up Resource: ${params.setUpResourceId}"
        log.info "Blotnum: ${params.blotnum}"
        // log.info "Need Stimulus Action: ${params.stimulus}"
        log.info "Need Randomization: ${params.randomization}"
        log.info "min1: ${params.min1}"
        log.info "min2: ${params.min2}"
        log.info "min3: ${params.min3}"
        session.removeAttribute("setUpResourceIdUpdate")
        session.removeAttribute("setUpResourceNameUpdate")
        File setupFile = session.getAttribute("setupupdate")
        def experiment=Experiment.get(params.experimentId)
        def blotnum=params.blotnum
        def resourcesList=experiment.resources.findAll{(it.type=="rawdata")&&(it.state=="active")}
        if(resourcesList.size()>0){
            render(text: """<script type="text/javascript"> warningMessage('Sorry, you already have active raw data files, means you already started the experiment. You can only update the set up before you really start the experiment.'); </script>""", contentType: 'text/javascript')           
        }else{
            try{
                def setUpTemplate=Template.get(params.setuptemplate)
   
       
                if (setupFile==null) {
                    log.error "no file uploaded yet"
                    render(text: """<script type="text/javascript"> warningMessage('no file uploaded yet'); </script>""", contentType: 'text/javascript')

                }else{
   
                    // def experiment = new Experiment()
                    def errorMessage="unknown"
             
                    log.info "$experiment"
                    def stages=experiment.stages
                    log.info stages
                    def newSetupResource=Resource.findById(params.setUpResourceId)
                    def resourceName=newSetupResource.fileName
                
                    log.info "new set up resource $resourceName"
                
                    def loadingTemplate
                    def rawDataTemplate
                    def gelInspectorTemplate
                
                    if(params.loadingtemplate!="default"){
                        loadingTemplate= Template.get(params.loadingtemplate)
                    }else{
                        loadingTemplate= Template.findByTemplateName(INNERLOADINGTEMPLATENAME)
                    }
                    if(params.rawdatatemplate!="default"){
                        rawDataTemplate= Template.get(params.rawdatatemplate)
                    }else{
                        rawDataTemplate= Template.findByTemplateName(INNERRAWDATATEMPLATENAME) 
                    }
                    if(params.gelinspectortemplate!="default"){
                        gelInspectorTemplate= Template.get(params.gelinspectortemplate)
                    }else{
                        gelInspectorTemplate= Template.findByTemplateName(INNERGELINSPECTORTEMPLATENAME) 
                    }
                
                    reConfigParsers=false
                
                    Stage setUpStage=stages.find{it.stageIndex==1}
                    //println setUpStage.stageTemplate.templateName
                
                    if(setUpStage.stageTemplate!=setUpTemplate){
                        setUpStage.stageTemplate=setUpTemplate
                        setUpStage.save(flush:true) 
                        log.info" setup template changed"
                        reConfigParsers=true
                    
                    }
                    Stage loadingStage=stages.find{it.stageIndex==2}
                    if(loadingStage.stageTemplate!=loadingTemplate){
                        loadingStage.stageTemplate=loadingTemplate
                        loadingStage.save(flush:true) 
                        log.info" loading template changed"
                        reConfigParsers=true
                    }
                    Stage rawdataStage=stages.find{it.stageIndex==3}
                    if(rawdataStage.stageTemplate!=rawDataTemplate){
                        rawdataStage.stageTemplate=rawDataTemplate
                        rawdataStage.save(flush:true)
                        log.info" rawdata template changed"
                        reConfigParsers=true
                    }
                
                    Stage gelInspectorStage=stages.find{it.stageIndex==4}
                    if(gelInspectorStage.stageTemplate!= gelInspectorTemplate){
                        gelInspectorStage.stageTemplate=gelInspectorTemplate
                        gelInspectorStage.save(flush:true) 
                        log.info"gel template changed"
                        reConfigParsers=true
                    }
                
        
                    def currentSetUpResource=experiment.resources.find{(it.type=="setup")&&(it.state=="active")}
                    if(currentSetUpResource){
                        currentSetUpResource.state="inactive"
                        currentSetUpResource.save(flush:true)  
                    }
         
                    def resources=experiment.resources
                    def date=new Date()
                    resources.add(newSetupResource)
                    if(resourceName.endsWith("xlsx")){
                        experiment.contentType="xlsx"
                    }else{
                        experiment.contentType="xls" 
                    }
                    experiment.binaryData=newSetupResource.binaryData
                    experiment.resources=resources
                    experiment.setUpTemplateName=setUpTemplate.templateName
                    experiment.topic=params.experimentTopic
                
                    //        experiment.logFile=logFileName
                    experiment.save(failOnError: true)
          
                   
          
                    if(reConfigParsers==true){
                        log.info "you change the template also, so the parser need reconfiguration"
                        experimentParsersConfigService.config(experiment)  
                    }
                                      
                    
                    session.putAt("fileName",resourceName)
                    session.removeAttribute("parsedFile")
                    session.removeAttribute("parsedFileName")   
                    // lenneke change here to set up the parserDef
                    def fileToParse=setupFile
                    
                    def conditionMap=[:]
                    //we want to do this a certain number of times for the different blots ie 3 times
                    Integer.parseInt(blotnum).times{
                        println "blotNum $it"
                       
                        parserService.parseSpreadsheet(fileToParse,[experiment:experiment, parserType:"Lane Setup", randomization:params.randomization, min1:params.min1, min2:params.min2, min3:params.min3, blotNum:it+1, conditionMap:conditionMap])            //  
                        fileToParse=session.getAttribute("parsedFile")
                        conditionMap=session.getAttribute("conditionMap")
                    
                    }
                    experiment.binaryData=session.getAttribute("parsedFile")
         
                    def sheetUpdate = new SheetUpdate(entityName:"ExperimentLoading", state:"update", fileNameVersion:"loading sheet in the workbook",comment:"Automatically update lane loading", dateUpdated:new Date())     
                    experiment.addToUpdates(sheetUpdate)
                    experiment.save(flush:true)
                    def warning=session.getAttribute("warningMessage")
               
                    session.removeAttribute("setupupdate")
                    setupFile.delete()
                    session.removeAttribute("setUpResourceIdUpdate")
                    session.removeAttribute("experimentId")
                    session.removeAttribute("experimentName")
                    session.removeAttribute("warningMessage")
                    session.putAt("active", "0")
        
                    render(text: """<script type="text/javascript"> afterUpload(2,2); </script>""", contentType: 'text/javascript')
                    render(text: """<script type="text/javascript"> refreshTableSorter();</script>""", contentType: 'text/javascript')
                    if(warning!=null){
                        render(text: """<script type="text/javascript"> warningMessage('${warning}'); </script>""", contentType: 'text/javascript')
       
                    }
                }
            }catch(Exception e){
                setupFile.delete()
                session.removeAttribute("setup")
                session.removeAttribute("setUpResourceId")
                session.removeAttribute("experimentId")
                session.removeAttribute("warningMessage")
                session.removeAttribute("conditionMap")
                session.putAt("active", "0")
                if (e instanceof grails.validation.ValidationException){
                         
                    render(text: """<script type="text/javascript"> warningMessage('Please make sure the experiment name is unique.'); </script>""", contentType: 'text/javascript')
     
                    
                    
                }
                else if(e instanceof RuleViolateException){
                    log.error "rule violat"  
                    render(text: """<script type="text/javascript"> warningMessage('${e.getMessage()}'); </script>""", contentType: 'text/javascript')
          
                }else{
                    render(text: """<script type="text/javascript"> warningMessage('unknow exception, not successfull'); </script>""", contentType: 'text/javascript')
  
                }
          
            }
        }

        render(text: """<script type="text/javascript"> refreshTableSorter(); </script>""", contentType: 'text/javascript')

        render(template:"/ui/user/experiment", model:[experimentInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='new'}] )
    }
    
    //${remoteFunction(controller:'lab', action:'initialPerExp', params:'\'experimentName=\'+experimentName+\'&setuptemplate=\'+setuptemplate+\'&geltemplate=\'+geltemplate+\'&setUpResourceId=\'+setUpResourceId+\'&gelBlot1ResourceId=\'+gelBlot1ResourceId+\'&gelBlot2ResourceId=\'+gelBlot2ResourceId+\'&gelBlot1ResourceId=\'+gelBlot3ResourceId', update:'updateMe2', onLoading:'showSpinner()', onComplete:'hideSpinner()') };
   
    def initialPerExp(){

        def user = User.get(springSecurityService.principal.id)
        session.putAt("username",springSecurityService.authentication.name)
        log.info "user ${session.username}"
        log.info "ExperimentName: ${params.experimentName}"
        log.info "ExperimentTopic: ${params.experimentTopic}"
        log.info "Using setupTemplate: ${params.setuptemplate}"
        log.info "Using loadingTemplate: ${params.loadingtemplate}"
        log.info "Using rawdataTemplate: ${params.rawdatatemplate}"
        log.info "Using gelinspectorTemplate: ${params.gelinspectortemplate}"

        log.info "Set Up Resource: ${params.setUpResourceId}"

        session.removeAttribute("performedSetUpResourceId")
        session.removeAttribute("performedSetUpResourceName")

        
        File setupFile = session.getAttribute("performsetup")

        if(setupFile==null){
            log.error "set upfile missing"
            render(text: """<script type="text/javascript"> warningMessage('please upload set up file and gel inspector files (at least one blot)'); </script>""", contentType: 'text/javascript')    
          
        }else{
            def experiment = new Experiment()
            
            def setupResource=Resource.findById(params.setUpResourceId)
        
            def setUpTemplate=Template.get(params.setuptemplate)
            def setUpStage=new Stage(stageIndex:1, stageName:'setup', stageTemplate:setUpTemplate)
    
            def loadingTemplate
            def rawDataTemplate
            def gelInspectorTemplate
            if(params.loadingtemplate!="default"){
                loadingTemplate= Template.get(params.loadingtemplate)
            }else{
                loadingTemplate= Template.findByTemplateName(INNERLOADINGTEMPLATENAME)
            }
            def loadingStage=new Stage(stageIndex:2, stageName:'loading', stageTemplate:loadingTemplate)
   
        
            if(params.rawdatatemplate!="default"){
                rawDataTemplate= Template.get(params.rawdatatemplate)
            }else{
                rawDataTemplate= Template.findByTemplateName(INNERRAWDATATEMPLATENAME) 
            }

        
        
            def rawdataStage=new Stage(stageIndex:3, stageName:'rawdata', stageTemplate:rawDataTemplate)

            if(params.gelinspectortemplate!="default"){
                gelInspectorTemplate= Template.get(params.gelinspectortemplate)
            }else{
                gelInspectorTemplate= Template.findByTemplateName(INNERGELINSPECTORTEMPLATENAME) 
            }
            def gelInspectorStage=new Stage(stageIndex:4, stageName:'gelInspector', stageTemplate:gelInspectorTemplate)
        
        
            
            def resourceName=setupResource.fileName
            def resources=[]
            def date=new Date() 
            try{
           
                      
                resources.add(setupResource)
               
            
                experiment.filename=params.experimentName
         
                experiment.binaryData=setupResource.binaryData
                experiment.author=user
                experiment.createdOn=date
                experiment.resources=resources
                experiment.setUpTemplateName=setUpTemplate.templateName
                if(resourceName.endsWith("xlsx")){
                    experiment.contentType="xlsx"
                }else{
                    experiment.contentType="xls" 
                }
                experiment.addToStages(setUpStage)
                experiment.addToStages(loadingStage)
                experiment.addToStages(rawdataStage)
                experiment.addToStages(gelInspectorStage)   
            
                experiment.share="private"
                experiment.type="performed"
                experiment.topic=params.experimentTopic
                experiment.save(failOnError: true)
        
                 performedExperimentParsersConfigService.config(experiment)
                
  
                def sheetUpdate = new SheetUpdate(entityName:"ExperimentBackUp", state:"backup", fileNameVersion:"${setupResource.fileName}[version:${setupResource.fileversion?formatter.format(setupResource.fileversion):"old"}]", comment:"upload the setup files for performed experiment ${params.experimentName}", dateUpdated: setupResource.fileversion)
                experiment.addToUpdates(sheetUpdate)
   
                session.removeAttribute("performsetup")
    
                setupFile.delete()
         
                session.removeAttribute("experimentId")
                session.putAt("active", "1")
                render(text: """<script type="text/javascript"> afterUpload(3,3); </script>""", contentType: 'text/javascript')
                render(text: """<script type="text/javascript"> refreshTableSorter(); </script>""", contentType: 'text/javascript')
          
            }catch(Exception e){ 
                setupFile.delete()
                if(experiment){
                     performedExperimentParsersConfigService.delete(experiment)
                    if(experiment.resources){
                        def r = []
                        r += experiment.resources
                        r.each{resource->
                            experiment.removeFromResources(resource)
                            resource.delete()        
                        }
                    }else{
                        setupResource.delete()  
                
                    }
                    if(experiment.updates){
       
                        def u=[]
                        u+=experiment.updates
                        u.each{update->
                            experiment.removeFromUpdates(update)
                            update.delete()            
                        }
                    }
      
                    experiment.delete(flush: true)
                }else{
                     
                    setupResource.delete()  
       
                
                }
                log.error e.message
                if (e instanceof grails.validation.ValidationException){
                    render(text: """<script type="text/javascript"> warningMessage('Please make sure the experiment name is unique.'); </script>""", contentType: 'text/javascript')
       
                }else if(e instanceof  NullPointerException){
                    render(text: """<script type="text/javascript"> warningMessage('Please make sure you upload the right set up file and also make sure the template you told us is the template you really use. You can delete the experiment or update the set up file to try again'); </script>""", contentType: 'text/javascript')
        
                }else{
                    render(text: """<script type="text/javascript"> warningMessage('Some parsing errors happens, please check and try again. Exception message: $e.message'); </script>""", contentType: 'text/javascript')
          
                }
 
        
            }
            
        }
  
        render(text: """<script type="text/javascript"> refreshTableSorter(); </script>""", contentType: 'text/javascript')
        render(template:"/ui/user/performedexperiment", model:[experimentPInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='performed'}] )
      
    }  
    
    
    //    def mailToSeek(){
    //        def user = User.get(springSecurityService.principal.id)
    //        try{
    //       
    //            def experimentInstance = Experiment.get(params.id)
    //            def youremailaccount=params.email
    //          
    //            log.info "send ${params.id} to $youremailaccount"
    //        
    //            def filename="${experimentInstance.filename.trim().replaceAll("\\s+", "_")}.xls"
    //
    //            mailService.sendMail {
    //                multipart true
    //                from "noreply-virtualliver@dkfz-heidelberg.de"
    //                to "${params.email}" 
    //                cc "seek@virtual-liver.de"
    //                subject "${experimentInstance.filename}"
    //                body "from Excemplify User: ${springSecurityService.authentication.name}"
    //                attachBytes filename,'application/vnd.ms-excel', experimentInstance.binaryData
    //            }
    //            def sheetUpdate = new SheetUpdate(entityName:"ExperimentSentToVLN", state:"vln", comment:"setup files and gelinspector files for performed experiment ${params.experimentName} uploaded to SEEK(VLN)", dateUpdated: new Date())
    //            experimentInstance.addToUpdates(sheetUpdate)
    //        
    //            render(text: """<script type="text/javascript">alert("file uploaded to SEEK via Exemplify Tool User and To Yourself. Please Varify By Checking Your Email");</script>""", contentType: 'text/javascript') 
    //        }catch(Exception e){
    //            
    //            render(text: """<script type="text/javascript"> warningMessage('Exception occurs. ${e.getMessage()}'); </script>""", contentType: 'text/javascript')
    //          
    //        }
    //        if(params.type=="updateMe"){
    //            render(template:"/ui/user/experiment", model:[experimentInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='new'}] )   
    //        }else if(params.type=="updatePMe"){
    //            render(template:"/ui/user/performedexperiment", model:[experimentPInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='performed'}] )
    //        }
    //    }
    def initialExp(){
        println "start initialization of experiment"
        def user = User.get(springSecurityService.principal.id)
        def errorMessage="unknown"
        session.putAt("username",springSecurityService.authentication.name)
        log.info "user ${session.username}"
        log.info "ExperimentName: ${params.experimentName}"
        log.info "ExperimentTopic: ${params.experimentTopic}"
        log.info "Using setupTemplate: ${params.setuptemplate}"
        log.info "Using loadingTemplate: ${params.loadingtemplate}"
        log.info "Using rawdataTemplate: ${params.rawdatatemplate}"
        log.info "Using gelinspectorTemplate: ${params.gelinspectortemplate}"
        log.info "Set Up Resource: ${params.setUpResourceId}"
        log.info "BlotNum: ${params.blotnum}"
        // log.info "Need Stimulus Action: ${params.stimulus}"
        log.info "Need Randomization: ${params.randomization}"
        log.info "min1: ${params.min1}"
        log.info "min2: ${params.min2}"
        log.info "min3: ${params.min3}"
        session.removeAttribute("setUpResourceId")
        session.removeAttribute("setUpResourceName")
        /**
         *  put following stuff into stages
         */
    
   def blotnum=params.blotnum
        def setUpTemplate=Template.get(params.setuptemplate)
        def setUpStage=new Stage(stageIndex:1, stageName:'setup', stageTemplate:setUpTemplate)
    
        def loadingTemplate
        def rawDataTemplate
        def gelInspectorTemplate
        if(params.loadingtemplate!="default"){
            loadingTemplate= Template.get(params.loadingtemplate)
        }else{
            loadingTemplate= Template.findByTemplateName(INNERLOADINGTEMPLATENAME)
        }
        def loadingStage=new Stage(stageIndex:2, stageName:'loading', stageTemplate:loadingTemplate)
   
        
        if(params.rawdatatemplate!="default"){
            rawDataTemplate= Template.get(params.rawdatatemplate)
        }else{
            rawDataTemplate= Template.findByTemplateName(INNERRAWDATATEMPLATENAME) 
        }

        
        
        def rawdataStage=new Stage(stageIndex:3, stageName:'rawdata', stageTemplate:rawDataTemplate)

        if(params.gelinspectortemplate!="default"){
            gelInspectorTemplate= Template.get(params.gelinspectortemplate)
        }else{
            gelInspectorTemplate= Template.findByTemplateName(INNERGELINSPECTORTEMPLATENAME) 
        }
        def gelInspectorStage=new Stage(stageIndex:4, stageName:'gelInspector', stageTemplate:gelInspectorTemplate)
  
    
        
        
        /**
         *
         */

        File setupFile = session.getAttribute("setup")
        if (setupFile==null) {
            log.error "no file uploaded yet"
            render(text: """<script type="text/javascript"> warningMessage('no file uploaded yet'); </script>""", contentType: 'text/javascript')    
          
        }else{
        
            def experiment = new Experiment()
            def setupResource=Resource.findById(params.setUpResourceId)
            def resourceName=setupResource.fileName
            def resources=[]
            def date=new Date()
            resources.add(setupResource)
            experiment.filename=params.experimentName
            experiment.binaryData=setupResource.binaryData
            experiment.author=user
            experiment.createdOn=date
            experiment.resources=resources
            if(resourceName.endsWith("xlsx")){
                experiment.contentType="xlsx"
            }else{
                experiment.contentType="xls" 
            }
            experiment.setUpTemplateName=setUpTemplate.templateName
            experiment.addToStages(setUpStage)
            experiment.addToStages(loadingStage)
            experiment.addToStages(rawdataStage)
            experiment.addToStages(gelInspectorStage)   
            experiment.share="private"
            experiment.type="new"
            experiment.topic=params.experimentTopic
            experiment.save(failOnError: true)
            //        experiment.logFile=logFileName
            
            experimentParsersConfigService.config(experiment)
            
            
            try{
  
                def sheetUpdate = new SheetUpdate(entityName:"ExperimentSetUp", state:"add/create", fileNameVersion:"${setupResource.fileName}[version:${setupResource.fileversion?formatter.format(setupResource.fileversion):"old"}]", comment:"upload the setup file and initial ${params.experimentName}", dateUpdated: setupResource.fileversion)
                experiment.addToUpdates(sheetUpdate)
                experiment.save()
                session.putAt("fileName",resourceName)
                session.removeAttribute("parsedFile")
                session.removeAttribute("parsedFileName")     
                // lenneke change here to set up the parserDef
                // instead should pass in the template name
                def fileToParse=setupFile
                //we want to do this a certain number of times for the different blots ie 3 times
                def conditionMap=[:]
                //we want to do this a certain number of times for the different blots ie 3 times
                Integer.parseInt(blotnum).times{
                    println "blotNum $it"
                       
                    parserService.parseSpreadsheet(fileToParse,[experiment:experiment, parserType:"Lane Setup", randomization:params.randomization, min1:params.min1, min2:params.min2, min3:params.min3, blotNum:it+1, conditionMap:conditionMap])            //  
                    fileToParse=session.getAttribute("parsedFile")
                    conditionMap=session.getAttribute("conditionMap")
                    
                }
                if(session.getAttribute("parsedFile")){
                    experiment.binaryData=session.getAttribute("parsedFile")
            
                    sheetUpdate = new SheetUpdate(entityName:"ExperimentLoading", state:"auto generate", fileNameVersion:"loading sheet in the workbook",comment:"Automatically generate lane loading", dateUpdated:new Date())
                    experiment.addToUpdates(sheetUpdate)
       
                    experiment.save(failOnError: true)
                }
                
                def warning=session.getAttribute("warningMessage")
                   
                session.removeAttribute("setup")
                setupFile.delete()
                session.removeAttribute("setUpResourceId")
                session.removeAttribute("experimentId")
                session.removeAttribute("warningMessage")
                session.removeAttribute("conditionMap")
                session.putAt("active", "0")
                if(warning!=null){
                    render(text: """<script type="text/javascript"> warningMessage('${warning}'); </script>""", contentType: 'text/javascript')
       
                }
                render(text: """<script type="text/javascript"> afterUpload(1,1); </script>""", contentType: 'text/javascript')
                render(text: """<script type="text/javascript"> refreshTableSorter();</script>""", contentType: 'text/javascript')
          
            }catch(Exception e){
                
                setupFile.delete() 
                log.error e.getMessage()
                session.removeAttribute("setup")
                session.removeAttribute("setUpResourceId")
                session.removeAttribute("experimentId")
                session.removeAttribute("warningMessage")
                session.removeAttribute("conditionMap")
                session.putAt("active", "0")
                
                if(experiment){
                    experimentParsersConfigService.delete(experiment)
                       
                    if(experiment.resources){
                        def r = []
                        r += experiment.resources
                        r.each{resource->
                            experiment.removeFromResources(resource)
                            resource.delete()        
                        }
                    }else{
                        setupResource.delete()  
                    }
                    if(experiment.updates){           
                        def u=[]
                        u+=experiment.updates             
                        u.each{update->
                       
                            experiment.removeFromUpdates(update) 
                            if(update){
                                update.delete()   
                            }
                        }
                    }  
             
                    experiment.delete(flush: true)
                }else{
                    setupResource.delete()   
                }   
            
                if (e instanceof grails.validation.ValidationException){
                    render(text: """<script type="text/javascript"> warningMessage('Please make sure the experiment name is unique.'); </script>""", contentType: 'text/javascript')
       
                }else if(e instanceof  NullPointerException){
                    render(text: """<script type="text/javascript"> warningMessage('Please make sure you upload the right set up file and also make sure the template you told us is the template you really use. You can delete the experiment or update the set up file to try again'); </script>""", contentType: 'text/javascript')
        
                }else if(e instanceof RuleViolateException ){
                    log.error "rule violat"  
                    render(text: """<script type="text/javascript"> warningMessage('${e.getMessage()}'); </script>""", contentType: 'text/javascript')
          
                }else{
                    render(text: """<script type="text/javascript"> warningMessage('Some parsing errors happens, please check and try again'); </script>""", contentType: 'text/javascript')
          
                }
 
        
            }
        }

        render(text: """<script type="text/javascript"> refreshTableSorter(); </script>""", contentType: 'text/javascript')
        
        render(template:"/ui/user/experiment", model:[experimentInstanceList:Experiment?.findAllByAuthor(user).findAll{it.type=='new'}] )
   
    }
    
    def warning(){
        log.info "warning"
        render(template: "/ui/user/warning")
    }
    def returnToMain(){
        session.removeAttribute("setup")
        session.removeAttribute("setupupdate")
        session.removeAttribute("performsetup")
        session.removeAttribute('setUpResourceId')
        session.removeAttribute('setUpResourceName')
        session.removeAttribute('setUpResourceNameUpdate')
        session.removeAttribute('setUpResourceIdUpdate')
        session.removeAttribute("performedSetUpResourceId")
        session.removeAttribute("performedSetUpResourceName")
        
        
        session.removeAttribute("rawdata")
        session.removeAttribute("rawdatamap")
        session.removeAttribute("geldata")
        session.removeAttribute("geldatamap")
        session.removeAttribute("experimentId")
        session.removeAttribute("experimentName")
        session.removeAttribute("info")
     
        session.removeAttribute("fileName")
        session.removeAttribute("parsedFile")
        session.removeAttribute("parsedFileName") 
        session.removeAttribute("openFileName") 
        session.removeAttribute("conditionMap")
        session.removeAttribute("file") 
        session.removeAttribute("active") 
        session.removeAttribute("xml") 
        session.removeAttribute("filepath")
        session.removeAttribute("templateType") 
        redirect(uri:"/")
    }
    def clear(){
        def after=params.after

        File setupFile=session.getAttribute("setup")
        File setupUpdateFile=session.getAttribute("setupupdate")
        File performedSetupFile=session.getAttribute("performsetup")
        if(setupFile){
            setupFile.delete();
            log.info "setup delete"
        }
        if(setupUpdateFile){
            setupUpdateFile.delete();
            log.info "setupupdate delete"           
        }
        
        if( performedSetupFile){
            performedSetupFile.delete();
            log.info "performed setup delete"
        }
        session.removeAttribute("setup")
        session.removeAttribute("setupupdate")
        session.removeAttribute("performsetup")
        
        def setup=session.getAttribute("setUpResourceId")
        if(setup){
            def setupResource=Resource.findById(setup)
            if(setupResource){
                setupResource.delete() 
            }
        }
        def setupupdate=session.getAttribute("setUpResourceIdUpdate")
        if(setupupdate){
            def setupUpdateResource=Resource.findById(setupupdate)
            if(setupUpdateResource){
                setupUpdateResource.delete() 
            }
        }
        def psetup=session.getAttribute("performedSetUpResourceId")
        if(psetup){
            def performedSetupResource=Resource.findById(psetup)
            if(performedSetupResource){
                performedSetupResource.delete() 
            }
        }
        session.removeAttribute('setUpResourceId')
        session.removeAttribute('setUpResourceName')
        session.removeAttribute('setUpResourceNameUpdate')
        session.removeAttribute('setUpResourceIdUpdate')
        session.removeAttribute("performedSetUpResourceId")
        session.removeAttribute("performedSetUpResourceName")
        
        
        session.removeAttribute("rawdata")
        session.removeAttribute("rawdatamap")
        session.removeAttribute("geldata")
        session.removeAttribute("geldatamap")
        session.removeAttribute("experimentId")
        session.removeAttribute("experimentName")
        session.removeAttribute("info")
     
        session.removeAttribute("fileName")
        session.removeAttribute("parsedFile")
        session.removeAttribute("parsedFileName") 
        session.removeAttribute("openFileName") 
        session.removeAttribute("conditionMap")
        session.removeAttribute("file") 
        session.removeAttribute("active") 
        session.removeAttribute("xml") 
        render(text: """<script type="text/javascript"> afterUpload($after,$after); </script>""", contentType: 'text/javascript')
        // redirect(uri:"/")
    }
    
    
    
    def updateupload(){
        log.info "experimentName: ${params.experimentName}"
        log.info "experimentId: ${params.experimentId}"
        log.info "update upload set up"
        def experimentName=params.experimentName
        def experimentId=params.experimentId
        def experiment=Experiment.get(experimentId)
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
        def f = request.getFile('mySetUpUpdate')
        def filePath=savePath
        if(!f.empty) {
            print "upload update "+f.getOriginalFilename()
            if(!f.getOriginalFilename().toString().endsWith(".xlsx")){
                def fileName=f.getOriginalFilename()
         
                filePath=filePath+"/"+System.nanoTime().toString()+"${f.getOriginalFilename()}"
                File newFile=new File(filePath)
                f.transferTo(newFile)
            
                session.putAt("setupupdate", newFile)
            
                def user=User.get(springSecurityService.principal.id)
    
                def  setUpResource=new Resource(fileName:fileName, type:"setup", binaryData:newFile.bytes, author:user, state:"active", fileversion: new Date());            
                setUpResource.save();
                
                def sheetUpdate = new SheetUpdate(entityName:"ExperimentSetUp",fileNameVersion:"${setUpResource.fileName}[version:${setUpResource.fileversion}]", state:"update", comment:"update the setup file  ${params.experimentName}", dateUpdated:setUpResource.fileversion)
                experiment.addToUpdates(sheetUpdate)
                if(experiment.contentType==null){
                    experiment.contentType="xls"
                }
                experiment.save(failOnError: true)
            
                log.info "resourceId ${setUpResource.id}"
                session.putAt("setUpResourceIdUpdate",setUpResource.id )
                session.putAt("setUpResourceNameUpdate",fileName )
                session.putAt("experimentName",experimentName )
                session.putAt("experimentId",experimentId)
                session.putAt("active", "0")
                session.putAt("selector", "update")
        
            }else{
                flash.message = 'Sorry! currently we only accept .xls setup file (for raw datafile you do not have such restriction), because of some nested memory problem from the programming library we used.'    
            }
        
        }
        else {
            flash.message = 'file cannot be empty'
        }

        log.info "Resource: $Resource.count"
  
        redirect(uri:"/lab")

    }
    
    
    
    def uploadperformed(){
        log.info "upload performed"
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
        def f1 = request.getFile('myPerformedSetUp')
      
        
        def filePath1=savePath
  
        if(!f1.empty){
            print "upload1 "+f1.getOriginalFilename()
            if(!f1.getOriginalFilename().toString().endsWith(".xlsx")){
     
            
                def fileName1=f1.getOriginalFilename()   
         
                filePath1=filePath1+"/"+System.nanoTime().toString()+"${f1.getOriginalFilename()}"
           
            
                File newFile1=new File(filePath1)
 
            
                f1.transferTo(newFile1)
        
            
                session.putAt("performsetup", newFile1)
       
                def user=User.get(springSecurityService.principal.id)
            
                def  setUpResource=new Resource(fileName:fileName1, type:"setup", binaryData:newFile1.bytes, author:user, state:"active", fileversion:new Date());      
           
                setUpResource.save();
          
            
                log.info "resourceIds ${setUpResource.id}"
        
                session.putAt("performedSetUpResourceId",setUpResource.id )
                session.putAt("performedSetUpResourceName",fileName1)
                session.putAt("active", "1")
                session.putAt("selector", "performed")
        
            
            }else{
                flash.message = 'currently we only accept .xls setup file (for raw datafile you do not have such restriction), because of some nested memory problem from the programming library we used.'    
            }
        
        }
        else {
            flash.message = 'all files cannot be empty'
        }

        log.info "Resource: $Resource.count"
        redirect(uri:"/lab") 
    }
    
    
    def upload(){

        log.info "upload set up"
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
        def f = request.getFile('mySetUp')
        def filePath=savePath
        if(!f.empty) {
            print "upload "+f.getOriginalFilename()
            if(!f.getOriginalFilename().toString().endsWith(".xlsx")){
                def fileName=f.getOriginalFilename()
         
                filePath=filePath+"/"+System.nanoTime().toString()+"${f.getOriginalFilename()}"
                File newFile=new File(filePath)
                f.transferTo(newFile)
            
                session.putAt("setup", newFile)
            
                def user=User.get(springSecurityService.principal.id)
                def  setUpResource=new Resource(fileName:fileName, type:"setup", binaryData:newFile.bytes, author:user, state:"active", fileversion:new Date());            
                setUpResource.save();
                log.info "resourceId ${setUpResource.id}"
                session.putAt("setUpResourceId",setUpResource.id )
                session.putAt("setUpResourceName",fileName )
                session.putAt("active", "0")
                session.putAt("selector", "new")
            
            }else{
                flash.message = 'Sorry! currently we only accept .xls setup file (for raw datafile you do not have such restriction), because of some nested memory problem from the programming library we used.'    
            }
        
        }
        else {
            flash.message = 'file cannot be empty'
        }

        log.info "Resource: $Resource.count"
        redirect(uri:"/lab")
    

    }


}
