package org.hits.ui

class Stage {
    static belongsTo = [experiment:Experiment]
    int stageIndex
    String stageName
    Template stageTemplate

    static constraints = {
     stageName(inList:["setup","loading","rawdata","rawdatatext","gelInspector"])
    }
    
   
}
