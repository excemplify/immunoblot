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
package org.hits.parser
   

import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.web.context.request.RequestContextHolder
import javax.servlet.http.HttpSession
import groovy.util.Eval

import org.hits.parser.core.*
import org.hits.parser.excelimp.*
import org.hits.ui.Template
import org.hits.ui.Experiment
import org.hits.ui.ExperimentToParserDef
import org.hits.ui.exceptions.*

class ParserService {

    // static transactional = true   default is true

    def getAllParserNames(){
        def names=[]
        ParserDef.findAll().each{
            names<<it.name
        }
        return names
    }

       
    def parseSpreadsheet(byte[] fileData, params) throws ParsingException, RuleViolateException{
        
        InputStream is = new ByteArrayInputStream(fileData)
    
        File file = File.createTempFile(session.getAttribute("fileName"),"")
       
        file.bytes=fileData
        
        parseSpreadsheet(file, params)
         println"please delete"
        file.delete()
      
    }
    
    
    def parseSpreadsheet(File file, params ) throws ParsingException, RuleViolateException{
        String parserType = params.parserType      
  
        // Template template = params.template
               
        
        //add these to the state..perhaps just add all params to the state?
        HttpSession session = getSession()
        
      
        StateAndQueue state = session.getAttribute("StateAndQueue")?:new StateAndQueue()  
        state.state.experimentWorkbookFile=session.getAttribute("ExperimentWorkbook") //the current experiment results/planning/etc workbook
        println"parse triger ${file.size()}"
        state.state.file=file // the file for parsing
        state.state.fileName=session.getAttribute("fileName")
        println "params $params"
        println ("session file name${session.getAttribute("fileName")}")
        println "about to get the parser"
         
        Block rootBlock = createParserBlock(state,parserType,params)
        
        try{
       
           
            rootBlock.applyQueue(state)
        
            session.putValue("parsedFile",state.state.parsedFile)
            session.putValue("parsedFileName","parsed_"+state.state.fileName)
            if(state.state.warningMessage){
                session.putValue("warningMessage", state.state.warningMessage)
            }
            if(state.state.conditionMap){
                session.putValue("conditionMap", state.state.conditionMap)
            }
           
            return state
        }catch(Exception pex){
             if (pex instanceof RuleViolateException){
                println    pex.getMessage()  
                throw pex 
            }
            else {
                println    pex.message    
                throw pex 
            }
            println "rule violation caught applying queue"
         
        }
   

    }
    //    
    //    def parseSpreadsheet(String filepath) {
    //
    //        InputStream fis = new FileInputStream(filepath)
    //        parseSpreadsheet(fis)
    //        
    //    }
    // 
  
    def createParserBlock(StateAndQueue state, String parserName, params) throws ParserConfigException{
       
        Experiment experiment=params.experiment
        // def parserDef = TemplateToParserDef.findByTemplate(template).parserDefs.find{ it.nextStageName==parserName }
        def parserDef = ExperimentToParserDef.findByExperiment(experiment).parserDefs.find{ it.nextStageName==parserName }
       
        println "creating parser block for name: ${parserName}"
        println parserDef.parserConfigurations
        parserDef.parserConfigurations.each{
            println it.properties
        }
        Block rootBlock
        Block prevBlock = null
        parserDef.parserConfigurations.each{ it->
            def newmap=[:]
    
            it.properties.each{k,v-> newmap.put(k,v)}
            
            def configmap=newmap
            configmap.put("experiment",Experiment)
       
            if (params.randomization!=null){
                configmap.put("randomization",params.randomization)
                configmap.put("min1",params.min1)
                configmap.put("min2",params.min2)
                configmap.put("min3",params.min3)
            }
            if (params.blotNum!=null){
                configmap.put("blotNum",params.blotNum)
            }
            if(params.conditionMap!=null){
                configmap.put("conditionMap", params.conditionMap)
            }
            //iterate thru list of config mappings
            //may need to double check the tree-likeness of this structure
            Block block = new Block(name:it.name, parser:ParserFactory.getParser(state,configmap) )//configmap has to hold source, target etc
            if (prevBlock!=null){
                println "prevblock=${prevBlock.name}"
                if (configmap.nodeType=="right"){
                    prevBlock.right = block
                }
                else{
                    prevBlock.down=block
                }
            }
            else {//we have the root node
                rootBlock=block
            }
            prevBlock=block
        }
        rootBlock.buildQueue(state)
        
        return rootBlock
        
        
    }

    
    private HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }
  
    public getParserName(String name){
      
        switch(name){
        case "gelinspector":
            return "Raw Data to Gel Inspector"
            break
        case "setup":
            return "Lane Setup"
            break
        }
    }
}