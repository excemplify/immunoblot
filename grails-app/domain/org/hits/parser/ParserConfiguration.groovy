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


class ParserConfiguration {

    
    
    //String source  //if ; is used then multiply source exist
    //String target
    String action
    String parserType
    String name
    String nodeType
    List sources
    
    static hasMany=[sources :SourceDef]
    static hasOne=[target:TargetDef]
    //static belongsTo = [parserDef: ParserDef]
    static constraints = {
    }
}
