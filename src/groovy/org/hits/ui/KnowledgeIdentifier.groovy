/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hits.ui
import org.apache.poi.ss.util.CellReference

/**
 *
 * @author rongji
 */
class KnowledgeIdentifier {
    static boolean inMarkCellRange(String referenceRange, String unknownLocation){
        boolean inRange=false
        def pattern=/(\w)(\d+)/
        def matcher3 = "$unknownLocation" =~ pattern     
        int col=CellReference.convertColStringToIndex("${matcher3[0][1]}")
        int row= matcher3[0][2] as int
        
        if(referenceRange.indexOf(":")!=-1){
            
            String upperleft=referenceRange.tokenize(":").first()
      
            String downright=referenceRange.tokenize(":").last()
     
            def matcher = "$upperleft" =~ pattern
    
            int startcol=CellReference.convertColStringToIndex("${matcher[0][1]}")
            int startrow= matcher[0][2] as int
            //        
            def matcher2 = "$downright" =~ pattern
    
            int endcol=CellReference.convertColStringToIndex("${matcher2[0][1]}")
            int endrow= matcher2[0][2] as int
     
       
            if(((col>=startcol)&&(col<=endcol))&&((row>=startrow)&&(row<=endrow))){
                inRange=true
            }
        }else{
            println "auto case"
            def matcher = "$referenceRange" =~ pattern 
            int startcol=CellReference.convertColStringToIndex("${matcher[0][1]}")
            int startrow= matcher[0][2] as int
            println "reference col $startcol row $startrow drop at $col $row"
              if((col==startcol)&&(row>=startrow)){
                inRange=true
            }
            
            
        }
        println "inRange: $inRange"
        return inRange
        
     
    }	
}

