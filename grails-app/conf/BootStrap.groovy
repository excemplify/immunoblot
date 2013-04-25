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
import grails.util.GrailsUtil
import org.hits.parser.User
import org.hits.parser.SecRole
import org.hits.parser.SecUser
import org.hits.parser.SecUserSecRole
import org.hits.parser.ParserDef
import org.hits.parser.ParserConfiguration
import org.hits.parser.excelimp.ImmunoParserAction
import org.hits.parser.ExcelSourceDef
import org.hits.parser.TextFileSourceDef
import org.hits.parser.TargetDef
import org.hits.parser.TemplateFile
import org.hits.ui.Template
import org.hits.ui.Knowledge
import org.hits.ui.Resource
import org.hits.ui.Experiment
import org.hits.ui.KnowledgeFetcher
import org.hits.ui.TemplateToParserDef
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.plugins.springsecurity.SecurityFilterPosition

class BootStrap {
    def authenticationManager
    def concurrentSessionController
    def securityContextPersistenceFilter
    def authenticationProcessingFilter
    def concurrentSessionControlStrategy

    def init = { servletContext ->
        switch(GrailsUtil.environment){
        case "test":
            initialize(servletContext)
            break

        case "production":
            initialize(servletContext)        
            dataclean();
        
            break
            
        case "development":
            initialize(servletContext)
            dataclean();
            break
        
        }
        
        SpringSecurityUtils.clientRegisterFilter('concurrencyFilter', SecurityFilterPosition.CONCURRENT_SESSION_FILTER)
        authenticationProcessingFilter.sessionAuthenticationStrategy = concurrentSessionControlStrategy
    }
    def destroy = {
    }
    
    def initialize(servletContext){  //some default
   
        def userRole = SecRole.findByAuthority('ROLE_USER') ?: new SecRole(authority: 'ROLE_USER').save(failOnError: true)
        def adminRole = SecRole.findByAuthority('ROLE_ADMIN') ?: new SecRole(authority: 'ROLE_ADMIN').save(failOnError: true)
        
        
        def adminUser = SecUser.findByUsername('jdoeadmin') ?: new SecUser(
            username: 'jdoeadmin',
            password: 'jdoeadmin',
            enabled: true).save(failOnError: true)
 
        if (!adminUser.authorities.contains(adminRole)) {
            SecUserSecRole.create adminUser, adminRole
        }
            
        def jdoe = SecUser.findByUsername('jdoe') ?: new User(username:"jdoe", password:"password", name:"John Doe",enabled: true).save(failOnError: true)
        def jsmith = SecUser.findByUsername('jsmith') ?: new User(username:"jsmith", password:"wordpass", name:"Jane Smith",enabled: true).save(failOnError: true)
      
        if (!jsmith.authorities.contains(userRole)) {
            SecUserSecRole.create jsmith, userRole
        }
         
        if (!jdoe.authorities.contains(userRole)) {
            SecUserSecRole.create jdoe, userRole
        }
 
        
        def webRootDir = servletContext.getRealPath("/")
        def templatePath = webRootDir+"template/";
        def setUpTemplateFilePath=templatePath+"setup_template_hits.xls";
        def rawDataTemplateFilePath=templatePath+"rawdata_template.xls";
        //            def laneloadingTemplateFilePath=templatePath+"laneloading_template.xls"
        def laneloadingTemplateNewFilePath=templatePath+"laneloading_template_new.xls"
       
        def testSetUpTemplate=Template.findByTemplateName("setup_template_hits.xls")?:new Template(templateName:"setup_template_hits.xls", binaryFileData:new File(setUpTemplateFilePath).bytes,  knowledgeList:[
                new Knowledge(knowledgeName:"Cells", columnName:"Cells", firstRow:"0", firstCol:"1", lastRow:"0", lastCol:"1", sheetIndex:"0", fileName:"setup_template_hits.xls", markCellRange:"B1", markColor:"ui-state-cell"),
                new Knowledge(knowledgeName:"Proteins", columnName:"Protein detection:", firstRow:"0", firstCol:"6", lastRow:"0", lastCol:"6", sheetIndex:"0", fileName:"setup_template_hits.xls", markCellRange:"G1", markColor:"ui-state-protein"),
                new Knowledge(knowledgeName:"TimePoints", columnName:"time points (min):", firstRow:"0", firstCol:"5", lastRow:"0", lastCol:"5", sheetIndex:"0", fileName:"setup_template_hits.xls", markCellRange:"F1", markColor:"ui-state-time"),
                new Knowledge(knowledgeName:"Doses", columnName:"treatment (ng/ml)", firstRow:"0", firstCol:"4", lastRow:"0", lastCol:"4", sheetIndex:"0", fileName:"setup_template_hits.xls", markCellRange:"E1", markColor:"ui-state-dose")          
            ], type:"public", purpose:"setup").save(failOnError: true);
    
        
        def testRawDataTemplate=Template.findByTemplateName("rawdata_template.xls")?:new Template(templateName:"rawdata_template.xls", binaryFileData:new File(rawDataTemplateFilePath).bytes,  knowledgeList:[
                new Knowledge(knowledgeName:"Bands", columnName:"Band No", firstRow:"0", firstCol:"1", lastRow:"0", lastCol:"1", sheetIndex:"0", fileName:"rawdata_template.xls", markCellRange:"B1", markColor:"ui-state-band"),
                new Knowledge(knowledgeName:"Lanes", columnName:"", firstRow:"0", firstCol:"0", lastRow:"0", lastCol:"0", sheetIndex:"0", fileName:"rawdata_template.xls", markCellRange:"A1", markColor:"ui-state-lane"),
                new Knowledge(knowledgeName:"Volumes", columnName:"Volume", firstRow:"0", firstCol:"2", lastRow:"0", lastCol:"2", sheetIndex:"0", fileName:"rawdata_template.xls", markCellRange:"C1", markColor:"ui-state-volume")
            ], type:"inner", purpose:"rawdata").save(failOnError: true)
            
   
   
                 
        def laneloadingTemplateNew=Template.findByTemplateName("laneloading_template_new.xls")?:new Template(templateName:"laneloading_template_new.xls", binaryFileData:new File(laneloadingTemplateNewFilePath).bytes,  
            knowledgeList:[new Knowledge(knowledgeName:"Lanes", columnName:"Lane", firstRow:"0", firstCol:"0", lastRow:"0", lastCol:"0", sheetIndex:"0", fileName:"laneloading_template_new.xls", markCellRange:"A1", markColor:"ui-state-lane"),
                new Knowledge(knowledgeName:"SampleNames", columnName:" Time(min) Cells Dose(ng/ml)", firstRow:"0", firstCol:"2", lastRow:"0", lastCol:"2", sheetIndex:"0", fileName:"laneloading_template_new.xls", markCellRange:"B1", markColor:"ui-state-samples")
            ],type:"inner", purpose:"loading").save(failOnError: true);

   
        //            def laneloadingTemplate=new Template(templateName:"laneloading_template.xls", binaryFileData:new File(laneloadingTemplateFilePath).bytes,  
        //                knowledgeList:[new Knowledge(knowledgeName:"Lanes", columnName:"Lane", firstRow:"0", firstCol:"0", lastRow:"0", lastCol:"0", sheetIndex:"0", fileName:"laneloading_template.xls", markCellRange:"A1", markColor:"ui-state-lane"),
        //                    new Knowledge(knowledgeName:"SampleNames", columnName:"", firstRow:"0", firstCol:"2", lastRow:"0", lastCol:"2", sheetIndex:"0", fileName:"laneloading_template.xls", markCellRange:"B1", markColor:"ui-state-samples")
        //                ],type:"inner").save(failOnError: true);
        //            
      
        def gelInspectorTemplate = Template.findByTemplateName("gelInspectorTemplate")?:new Template(templateName:"gelInspectorTemplate", binaryFileData: new File(webRootDir,'template/gelInspectorTemplate.xls').bytes,  knowledgeList:[],type:"inner",purpose:"gelInspector").save(failOnError: true)
     

        def rawTextDataTemplate = Template.findByTemplateName("rawTextDataTemplate")?:new Template(templateName:"rawTextDataTemplate", binaryFileData: new File(webRootDir,'template/rawTextDataTemplate.txt').bytes,  knowledgeList:[],type:"inner",purpose:"rawdata text").save(failOnError: true)

        
            
        log.info Knowledge.count();
        log.info Template.count();
    }
    
    def dataclean(){
        log.info "start data cleaning"
        def knowledgeFileNames=Knowledge.executeQuery("select distinct fileName from Knowledge" )
        def templateNames=Template.executeQuery("select distinct templateName from Template" )
        def wnum=0
        knowledgeFileNames.each{name->
            if(!templateNames.contains(name)){
                def wastKnowledgeList=Knowledge.findAllByFileName(name); 
                wastKnowledgeList.each{wast->
                    def id=wast.id
                    wast.delete()
                    wnum=wnum+1
                 
                }
            }               
        }
            
        log.info "delete  $wnum wast knowledges"
            
        def resourceIds=Resource.executeQuery("select distinct id from Resource" ) 
        def experimentResources=Experiment.executeQuery("select resources from Experiment" )
        def num=0
        resourceIds.each{id->
            def resource=Resource.get(id)
            if(!experimentResources.contains(resource)){
                resource.delete()
                num=num+1
            }
                
        }
            
        log.info "$num waste resources deleted"
    }
}
