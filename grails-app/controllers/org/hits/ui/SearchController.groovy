

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

import org.hits.ui.rdf.SPARQLEngine
import org.hits.ui.*
import org.hits.parser.*
class SearchController {
    def springSecurityService
    def index() { }
    
    def sparqlQuery(){
        println "quering"
        def user = User.get(springSecurityService.principal.id)
        def userid=user.id
        def webRootDir = servletContext.getRealPath("/")
        def savePath = webRootDir+"rdf/"
        String queryString ="PREFIX excemplify:<http://hits.org/ontology/lab#> SELECT ?exp ?expn ?id WHERE { ?exp excemplify:hasMetaData ?meta. ?exp excemplify:hasName ?expn. ?exp excemplify:hasID ?id. ?meta excemplify:isA 'Treatments'. ?meta excemplify:hasValue ?v. FILTER regex(?v, '(HGF+)','i')}";
        SPARQLEngine engine=new SPARQLEngine(savePath.toString(), userid.toString(), queryString)
        //     String queryString2 ="PREFIX j.0:<http://hits.org/ontology/excemplifymeta#> SELECT ?exp ?id WHERE {  ?exp j.0:hasID ?id. ?exp j.0:hasName 'tu'}";
        //        
        //        SPARQLEngine engine2=new SPARQLEngine(savePath.toString(), userid.toString(), queryString2)
        
        
    }
       
    def sparqlSearch(){
        println "searching"
      //  sparqlQuery()
        def user = User.get(springSecurityService.principal.id)
        def userid=user.id
        def webRootDir = servletContext.getRealPath("/")
        def savePath = webRootDir+"rdf/"
        String prefix ="PREFIX excemplify:<http://hits.org/ontology/lab#> "
        String prefix2="PREFIX afn:<http://jena.hpl.hp.com/ARQ/function#> PREFIX fn:<http://www.w3.org/2005/xpath-functions#> "
       String queryString =prefix+prefix2+ params.querystring
      // String queryString ="PREFIX excemplify:<http://hits.org/ontology/meta#> SELECT ?id WHERE {?exp excemplify:hasID ?id. ?exp excemplify:hasMetaData ?m. ?m excemplify:isA 'Proteins'. ?m excemplify:hasValue ?v. FILTER regex(?v, '(smad2)*','i').}"
        println "queryString: $queryString"
         try{
        SPARQLEngine engine=new SPARQLEngine(savePath.toString(), userid.toString(), queryString)
        def resultsIds=engine.getResultIds()
        def experimentResultList=[] as Set
        resultsIds.each{id->
           Experiment exp=Experiment.get(id as int)
           if(exp)
            experimentResultList << exp
        }
        println experimentResultList
       render(text: """<script type="text/javascript"> clearSearchArea(); </script>""", contentType: 'text/javascript')
       render(text: """<script type="text/javascript"> refreshTableSorter(); </script>""", contentType: 'text/javascript')
         render(template:"/ui/user/resultexperiment", model:[experimentResults:experimentResultList] )
         }catch (Exception e){
                 flash.message = e.getMessage()
            redirect(uri:"/exception")
         }

    }
}
