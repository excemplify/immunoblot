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

package org.hits.ui
import org.hits.ui.exceptions.*

/**
 *
 * @author rongji
 */
class KnowledgeFetcher {
    static String cellRangeStringFetcher(Template template, String knowledgeName) throws Exception{
        def cellRangeString=""
       
        Knowledge cellKnowledge=template.knowledgeList.find{it.knowledgeName==knowledgeName}
        if(!cellKnowledge){
                
            println "oops, $knowledgeName Concept is one of the mandatory concepts but it is missing in such Template"
            throw new ConceptMissingException("$knowledgeName concept is one of the mandatory concepts, but the mapping for it is missing");
                
        }else{
            def matcher=cellKnowledge.markCellRange=~ /(\w)(\d+)/
            def cols=matcher[0][1]  
            def rows=matcher[0][2] 
            //        println rows
            //        println cols
            def nextRows=Integer.valueOf(rows)+1
            cellRangeString="$cols$nextRows"
            println cellRangeString 
        }
      
       
        return cellRangeString       
    }
    
    
    static boolean containKnowledge(Template template, List knowledgeNames){
        boolean flag=true
           knowledgeNames.each{k->
               println k
             Knowledge cellKnowledge=template.knowledgeList.find{it.knowledgeName==k}  
             if(!cellKnowledge){
                 println " no knowledge found return null"
                 flag=false
                
             }
           }
        
      return flag
          
      
    }

}

