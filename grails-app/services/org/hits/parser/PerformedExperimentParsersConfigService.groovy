
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
import org.hits.ui.Experiment
import org.hits.ui.Stage
import org.hits.ui.Template
import org.hits.ui.ExperimentToParserDef
import org.hits.parser.ParserDef
import org.hits.parser.ParserConfiguration
import org.hits.parser.excelimp.ImmunoParserAction
import org.hits.parser.ExcelSourceDef
import org.hits.parser.TextFileSourceDef
import org.hits.parser.TargetDef
import org.hits.ui.Knowledge
import org.hits.ui.Resource
import org.hits.ui.KnowledgeFetcher

class PerformedExperimentParsersConfigService {

    static INNERRAWDATATEMPLATENAME="rawdata_template.xls"
    static INNERGELINSPECTORTEMPLATENAME="gelInspectorTemplate_0723.xls"
    
    def delete(Experiment experiment){
        def configs=ExperimentToParserDef.findAllByExperiment(experiment)
        configs.each{config->
            deleteExperimentToStagesToDefs(config)     
        }        
    }
    
    def defaultConfig(Experiment experiment){
        /**
         *for performed experiment, the generation only happens from the rawdata stage. 
         */
        if(!experiment.stages){
         
            def rawDataTemplate= Template.findByTemplateName(INNERRAWDATATEMPLATENAME) 
            def gelInspectorTemplate= Template.findByTemplateName(INNERGELINSPECTORTEMPLATENAME)   
            
            def rawdataStage=new Stage(stageIndex:1, stageName:'rawdata', stageTemplate:rawDataTemplate)      
            def gelInspectorStage=new Stage(stageIndex:2, stageName:'gelInspector', stageTemplate:gelInspectorTemplate)
           
            experiment.addToStages(rawdataStage)
            experiment.addToStages(gelInspectorStage)  
            experiment.save(failOnError: true)          
        }
        config(experiment)
    }
    
    def config(Experiment experiment) {
        //
        println "start config parsers for such experiment $experiment.id"
        def experimentToParserDefInstance
        def stages=experiment.stages
        println stages
        def indexes=stages.collect{it.stageIndex}
        int minIndex=indexes.min()

        int maxIndex=indexes.max()
   
        for (int i=minIndex ; i<maxIndex; i++){
            
            /*
             *clean dirty stuff
             */
            Stage curStage=stages.find{it.stageIndex==i}
            Stage posStage=stages.find{it.stageIndex==i+1}
            
            experimentToParserDefInstance=ExperimentToParserDef.findByExperiment(experiment)
            if(experimentToParserDefInstance){
                deleteExperimentToStagesToDefs(experimentToParserDefInstance)
            }       
            def parserDefInstance=ParserDef.findByName("${experiment.id}${curStage.stageName}To${posStage.stageName}")
            if(parserDefInstance){
                deleteDef(parserDefInstance)
            }
            
            
        }
        
      
        
        experimentToParserDefInstance=preDefineStagesParsers(experiment)
        log.info " ExperimentToParserDefs ${experimentToParserDefInstance}"
        
        
        
        

    }
    ExperimentToParserDef preDefineStagesParsers(Experiment experiment){
        
        
     
        Template rawdataTemplate=experiment.stages.find{it.stageIndex==3}.stageTemplate
        Template gelInspectorTemplate=experiment.stages.find{it.stageIndex==4}.stageTemplate
        
        // exception made for their .text rawdata template coming out from the old machine
        Template rawdataTextTemplate=Template.findByTemplateName("rawTextDataTemplate")
        def rawdataSources=[]
   
        def rawDataToGelInspectorDefInstance = new ParserDef(name:"${experiment.id}rawdataTogelInspector", nextStageName: "Raw Data to Gel Inspector", parserConfigurations:[
                new ParserConfiguration( sources:[new ExcelSourceDef(sourceType:"CURRWORKBOOK", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(rawdataTemplate, 'Lanes')}", sheetNum:0,sheetName:"not_used",template:rawdataTemplate),//lane
                        new ExcelSourceDef(sourceType:"CURRWORKBOOK", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(rawdataTemplate, 'Volumes')}", sheetNum:0,sheetName:"",template:rawdataTemplate), //volume
                        new ExcelSourceDef(sourceType:"CURRWORKBOOK", cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(rawdataTemplate, 'Bands')}",sheetNum:0,sheetName:"",template:rawdataTemplate)],// band Number
                    action:ImmunoParserAction.COPY.toString(),
                    parserType:"RawDataExcelParser",
                    name:"laneAndVolume",
                    nodeType:"root",
                    target:hasRequiredKnowledges(gelInspectorTemplate, ["Lanes"])?new TargetDef(cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(gelInspectorTemplate, 'Lanes')}",sheetNum:0, sheetName:"Gel Inspector", name:"RawDataLane", template:gelInspectorTemplate ):new TargetDef(cellRange:"a2",sheetNum:0, sheetName:"Gel Inspector", name:"RawDataLane", template:gelInspectorTemplate ))])
         
        
        //exception parser made for their .text rawdata template coming out from the old machine
        def rawTextDataToRawDataInstance = new ParserDef(name:"${experiment.id}rawdatatextTorawdata",nextStageName: "Raw Data (Text) to Excel", parserConfigurations:[
                new ParserConfiguration(sources:[new TextFileSourceDef(dataLabel:"Lanes", sourceType:"rawdatatext", startRegexp:"Lane",endRegexp:"r1"),
                        new TextFileSourceDef(dataLabel:"Volume", sourceType:"rawdatatext", startRegexp:"r",endRegexp:"Sum")],
                    parserType:"NonExcelRawDataParser", action:ImmunoParserAction.COPY.toString(), name:"rawdata1",nodeType:"down",
                    target:hasRequiredKnowledges(rawdataTemplate, ["Lanes"])?new TargetDef(cellRange:"${KnowledgeFetcher.cellRangeStringFetcher(rawdataTemplate, 'Lanes')}",sheetNum:0, sheetName:"Raw_Data", name:"RawDataLane", template:rawdataTemplate):new TargetDef(cellRange:"a2",sheetNum:0, sheetName:"Raw_Data", name:"RawDataLane", template:rawdataTemplate))])
         
        
        def experimentToParserDefInstance=new ExperimentToParserDef(experiment:experiment, parserDefs:[rawDataToGelInspectorDefInstance, rawTextDataToRawDataInstance] ).save(failOnError: true);
        return experimentToParserDefInstance                                                                
                               
    } 
    
    boolean hasRequiredKnowledges(Template template, List knowledgeNames){
        def flag=KnowledgeFetcher.containKnowledge(template, knowledgeNames)
        println "flag $flag"
        return flag
    }
    
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
    def deleteExperimentToStagesToDefs(  ExperimentToParserDef experimentToParserDefInstance){
        log.info "dirty experimentToStagesToParserDefInstance $experimentToParserDefInstance"
        // def templateParserInstance= TemplateToParserDef.findByTemplate(templateInstance)             
        if(experimentToParserDefInstance){                         
            def allParserDefs = []
            allParserDefs+= experimentToParserDefInstance.parserDefs
            log.info "parserDefs count ${allParserDefs.size()}"
            allParserDefs.each{pD->
                
                if(pD){
                    experimentToParserDefInstance.removeFromParserDefs(pD)
                    deleteDef(pD)                 
                }

              
            }
               
         
            experimentToParserDefInstance.delete()  
            log.info "templateParser $experimentToParserDefInstance.id delete"
        }
    }
    
    
    
}
