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

class Spreadsheet {

    

    String filename
    byte[] binaryData
    Date createdOn
    //Date[] updatedOn
    static belongsTo = [author:User]
    
    static hasMany = [updates:SheetUpdate]
    static mapping = {
        updates sort:'dateUpdated', order:'desc'
    }
    static constraints = { binaryData maxSize: 1024 * 1024 * 25 // 25MB which is the maximum attachment size in the mail tool }
    
    }



    def lastUpdate = {
        if (updates.iterator().hasNext()) {
            updates.iterator().next().dateUpdated
        }
        else return createdOn
    }
}
