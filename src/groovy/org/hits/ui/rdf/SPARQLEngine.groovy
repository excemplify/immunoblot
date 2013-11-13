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

/**
 *
 * @author rongji
 */
import com.hp.hpl.jena.query.*

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.RDFNode
import com.hp.hpl.jena.rdf.model.Literal
import com.hp.hpl.jena.util.FileManager
import com.hp.hpl.jena.rdf.model.Resource

class SPARQLEngine {
    
    // Model m = FileManager.get().loadModel( "./rdf/test.rdf" );
  
    File folder
    def resultIds=[] as Set
    
    SPARQLEngine(){
        
    }
    SPARQLEngine(String rdfRootPath, String queryString){  //search all
       
    }
    SPARQLEngine(String rdfRootPath, String userid, String queryString){  //only search special user
        folder = new File(rdfRootPath);
        Search(folder, userid, queryString)    
    }
    
    def getResultIds(){
        return this.resultIds
    }

    def Search(File folder, String userid, String queryString){
        def listOfFiles = folder.listFiles(); 
        println "scanning"
        listOfFiles.each{file->  
      
            if(file.getName().split("\\.")[1]==userid){
                Search(file, queryString)  
            }else{
                println "skip"
            }
        }
    }
    
    Resource SearchModel(Model model, String queryString){
        Resource r
        Query query = QueryFactory.create(queryString)
        // println query
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
                 ResultSetFormatter.out(System.out, qexec.execSelect(), query) ;
        try {
            com.hp.hpl.jena.query.ResultSet results = qexec.execSelect() 
            
            //resultValues=results.getResultVars()
            // println "has result ${results.size()}"
            // println resultValues
            while(results.hasNext()){
              
                QuerySolution soln =results.next()
                // println(soln.toString()); 
                // RDFNode x = soln.get("expn") ;       // Get a result variable by name.
              r = soln.getResource("resource") ;       // Get a result variable by name.
              
            }
  
                
        }catch(Exception e){
            // println "exception happens"
            println e
        }
        finally { qexec.close() ; }
        
        return r
    }
    def Search(File file, String queryString){
     
        Model m = FileManager.get().loadModel(file.getAbsolutePath());
        //  Query query = QueryFactory.create("PREFIX excemplify:<http://hits.org/ontology/meta#> SELECT ?exp ?expn WHERE {?exp excemplify:hasName ?expn. FILTER regex(?expn, 'tu','i')}") ;
        Query query = QueryFactory.create(queryString)
        // println query
        QueryExecution qexec = QueryExecutionFactory.create(query, m) ;
        // ResultSetFormatter.out(System.out, qexec.execSelect(), query) ;
        try {
            com.hp.hpl.jena.query.ResultSet results = qexec.execSelect() 
            
            //resultValues=results.getResultVars()
            // println "has result ${results.size()}"
            // println resultValues
            while(results.hasNext()){
              
                QuerySolution soln =results.next()
                // println(soln.toString()); 
                // RDFNode x = soln.get("expn") ;       // Get a result variable by name.
                RDFNode y = soln.get("id") ;       // Get a result variable by name.
                
                println "experiment with id ${y}"
                resultIds << y.toString()
    
         
            }
  
                
        }catch(Exception e){
            // println "exception happens"
            println e
        }
        finally { qexec.close() ; }
     
           
    }
}

