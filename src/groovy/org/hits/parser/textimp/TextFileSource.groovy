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

package org.hits.parser.textimp

import org.hits.parser.core.*
import org.hits.parser.SourceDef
/**
 *
 * @author jongle
 */
class TextFileSource implements Source{

    File textFile
    def sourceLines=[:]
    
    TextFileSource(List<SourceDef> sourcedefs,StateAndQueue state){
        
        textFile = state.state.file //assuming we have a file object in the state here.
        List<String> textFileLines = []
        textFile.eachLine{
            textFileLines<<it
        }
        
        sourcedefs.each{ sourcedef->
            //the strategy, find the line we are interested in by searching thru the list, matching the start with the sourcedef regexp
            // then....
            def pattern = /^$sourcedef.startRegexp/
            def lines = textFileLines.findAll{  it =~ pattern}//this only gets one list
           
            sourceLines.putAt(sourcedef.dataLabel,lines)
          
        }
        println sourceLines
    }
    
    def getSourceLines(){
        return sourceLines
    }
    
}

