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

package org.hits.parser.excelimp

import org.hits.parser.core.Action

/**
 *
 * @author jongle
 */
enum ImmunoParserAction implements Action{

    COPY("COPY"),
    TRANSPOSE("TRANSPOSE"),
    RECORDS("RECORDS"),
   
    SPLIT_COLUMNS("SPLIT_COLUMNS"),
    MERGE_COLUMNS("MERGE_COLUMNS"),
    ADD_COLUMN_DATA("ADD_COLUMN_DATA"),
    ADD_ROW_DATA("ADD_ROW_DATA"),

    OUTERPRODUCT_COLUMNS("OUTERPRODUCT_COLUMNS"), //lei try
    OUTERPRODUCT_COLUMNS_RANDOMIZE("OUTERPRODUCT_COLUMNS_RANDOMIZE"),
    OUTERPRODUCT_COLUMNS_MATCH("OUTERPRODUCT_COLUMNS_MATCH"),
    MATCH_AND_SPLIT("MATCH_AND_SPLIT")
    private String action
    
    ImmunoParserAction(String action){
       this.action=action
    }
    
    def getAction(){
        return action
    }
    
    String toString(){
        return action
    }
}

