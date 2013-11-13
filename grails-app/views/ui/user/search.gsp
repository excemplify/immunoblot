%{--===================================================
Copyright 2010-2013 HITS gGmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
limitations under the License.
    ========================================================== 
--}%

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.hits.ui.Template" %>
<%@ page import="org.hits.ui.Experiment" %>
<%@ page import="org.hits.parser.Spreadsheet" %>
<%@ page import="org.hits.parser.User" %>
<%@ page import="org.hits.parser.SecUser"%>
<%@ page import="grails.plugins.springsecurity.SpringSecurityService" %>
<% def springSecurityService %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

    <!--Required-->
    <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'index.css')}" />
    <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'search.css')}" />
    <link rel="stylesheet" type="text/css" href="${createLinkTo(dir:'css', file:'jquery.ui.selectmenu.css')}" />
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery-1.5.2.min.js')}"></script>
    <!--jQuery UI-->
    <script type="text/javascript" src="${createLinkTo(dir:'jquery-ui/ui', file:'jquery-ui.min.js')}"></script>
    <link rel="stylesheet" type="text/css" href="${createLinkTo(dir:'jquery-ui/sunny', file:'jquery-ui-1.8.20.custom.css')}" />
    <!--/jQuery UI-->
    <!--Required-->

    <script type="text/javascript" src="${createLinkTo(dir:'js/plugins', file:'jquery.elastic.min.js')}"></script>
    <!--/Elastic-->

    <!--jQuery Table Sort-->
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.tablesorter.pager.js')}"></script>
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.tablesorter.min.js')}"></script>
    <!--jQuery Table Sort-->
    <!--jQuery Table Select Menue-->
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.ui.selectmenu.js')}"></script>
    <!--jQuery Table Select Menue-->

    <title>Excemplify</title>
    <link rel="shortcut icon" href="${createLinkTo(dir: 'images/ui/', file: 'logo.ico')}" type="image/x-icon"/>
    <script type="text/javascript">
      var selectFieldQuery="";
        var selectQuery="";
        var filterQuery="";

       var firstQuery=true;
       var type="";
       var queryindex=0;
     window.onload = function(){
     
      var active=0;
        
        
      
         $("#warning-form").dialog({
                      autoOpen: false,
                        height: 400,
                        width: 400,
                        modal: true,
                        buttons:{
                           "Close": function() {
                              $("#errormessage").html("");
                              $( this ).dialog( "close" );
                                        
                                }
                        },
                       close: function() {
                                allFields.val( "" ).removeClass( "ui-state-error" );
                        }
        });
        
            $("#email-form").dialog({
                      autoOpen: false,
                        height: 300,
                        width: 400,
                        modal: true,
                        buttons:{
                           "Send": function() {
                           var emailaddress= $("#email").val(); 
                           var id= $("#sendSEEKExpId").html();
                           var type= $("#sendType").html();
${remoteFunction(controller:'experiment', action:'mailToSeek', params:'\'id=\'+id+\'&email=\'+emailaddress+\'&type=\'+type', update:[success:'feedback', failure:'feedback'], onFailure:'warning()', onLoading:'showSpinner()', onComplete:'hideSpinner()') };
                              $( this ).dialog( "close" );
                            $("#email").html(""); 
                          $("#sendSEEKExpId").html(""); 
                           $("#sendType").html(""); 
                                }
                        },
                       close: function() {
                                allFields.val( "" ).removeClass( "ui-state-error" );
                        }
        });
           
        $( "#resultaccordion" ).accordion({
                        collapsible: false,
                        autoHeight:true,
                        active:active
                });
 $('#type').val("Experiments");
  $('#operator').val("AND");
 $('#searchterms').val("ExperimentName");
  refreshTableSorter();
      formQueryString();
  
     }
     
 function formQueryString(){
 
   //alert(type);
   var queryString="";
 
     var subQuery="\?exp excemplify\:hasID \?id\."
     if(filterQuery==""){
       queryString=" SELECT \?id WHERE \{"+subQuery+selectFieldQuery+selectQuery+"\}"  
     }else{
        queryString=" SELECT \?id WHERE \{"+subQuery+selectFieldQuery+selectQuery+" FILTER("+filterQuery+")\}"
     }

     
   $('#sparqlarea').val(queryString);

 } 
 
function addToQueryAndSearch(){
  var queryString=""
     type=$('#type').val();
  var field=$('#searchterms').val()
  var operator=$('#operator').val()
  var term=$('#searchtermField').val().toLowerCase()
    var dfrom=$('#from').val()
     var dto=$('#to').val()
  var optionalString=""
  term.replace(/\+/g,'\+')
  
  var to=term.indexOf("%")
  if(to!=-1){
    term=term.substring(0, to);

  }
  
  if(operator=="AND"){ 
  
  if(field=="ExperiName"){
    if(to!=-1){
    
       selectQuery=selectQuery+" \?exp excemplify\:hasName \?expn\. FILTER regex(\?expn, '("+term+"\+)', 'i')\." ;
    }else{
        if(firstQuery==true){
                 filterQuery=filterQuery+"fn\:lower-case(\?expn\)='"+term+"' " 
        }else{
                 filterQuery=filterQuery+"\&\& fn\:lower-case(\?expn\)='"+term+"' " 
        }

        selectQuery=selectQuery+" \?exp excemplify\:hasName \?expn\. ";
    }

  }else if(field=="ExperiTopic"){
       if(to!=-1){
         selectQuery=selectQuery+" \?exp excemplify\:hasTopic \?topic\. FILTER regex(\?topic, '("+term+"\+)','i')\." ;
    }else{
      
      
         if(firstQuery==true){
                 filterQuery=filterQuery+"fn\:lower-case(\?topic\)='"+term+"' " 
        }else{
                 filterQuery=filterQuery+"\&\& fn\:lower-case(\?topic\)='"+term+"' " 
        }
      
         selectQuery=selectQuery+" \?exp excemplify\:hasTopic \?topic\. ";
    }
  }else if((field=="Cells")||(field=="Treatments")||(field=="TimePoints")||(field=="Proteins")||(field=="Antibody")){
    if(type=="Experiments"){
      if(to!=-1){
         selectQuery=selectQuery+" \?exp excemplify\:hasMetaData \?m"+queryindex+"\. \?m"+queryindex+" excemplify\:isA '"+field+"'\. \?m"+queryindex+" excemplify\:hasValue \?v"+queryindex+"\. FILTER regex(\?v"+queryindex+", '("+term+"\+)','i')\."; 
    }else{
      
    if(firstQuery==true){
                 filterQuery=filterQuery+"fn\:lower-case(\?v"+queryindex+"\)='"+term+"' " 
        }else{
                 filterQuery=filterQuery+"\&\& fn\:lower-case(\?v"+queryindex+"\)='"+term+"' " 
        }

         selectQuery=selectQuery+" \?exp excemplify\:hasMetaData \?m"+queryindex+"\. \?m"+queryindex+" excemplify\:isA '"+field+"'\. \?m"+queryindex+" excemplify\:hasValue \?v"+queryindex+"\." ;
    }
    }else if(type=="Data Points"){
      
      
          if(to!=-1){
         selectFieldQuery=" \?exp excemplify\:hasData \?d\."
  
         selectQuery=selectQuery+"  \?d excemplify\:has"+field+" \?v"+queryindex+"\. FILTER regex(\?v"+queryindex+", '("+term+"\+)','i')\."; 
    }else{
      
    if(firstQuery==true){
                 filterQuery=filterQuery+"fn\:lower-case(\?v"+queryindex+"\)='"+term+"' " 
        }else{
                 filterQuery=filterQuery+"\&\& fn\:lower-case(\?v"+queryindex+"\)='"+term+"' " 
        }
         selectFieldQuery=" \?exp excemplify\:hasData \?d\."
         selectQuery=selectQuery+" \?d excemplify\:has"+field+" \?v"+queryindex+"\." ;
    }
      
 
    } 
   }else if(field=="Data Value"){
   
     if(type=="Data Points") {
                   if(to!=-1){
         alert("no OR support for wildcard");
    }else{
      
    if(firstQuery==true){
                 filterQuery=filterQuery+"\?dv"+queryindex+"\>\="+dfrom+" \&\& \?dv"+queryindex+"\<\="+dto
        }else{
                  filterQuery=filterQuery+"\&\& \?dv"+queryindex+"\>\="+dfrom+" \&\& \?dv"+queryindex+"\<\="+dto
        }
         selectFieldQuery=" \?exp excemplify\:hasData \?d\."
         selectQuery=selectQuery+" \?d excemplify\:hasDataValue \?dv"+queryindex+"\." ;
    }
     }
     
   }
  }else if(operator=="OR"){
     if(field=="ExperiName"){
       if(to!=-1){
         
       alert("no OR support for wildcard");
     //  selectQuery=selectQuery+" \?exp excemplify\:hasName \?expn\. FILTER regex(\?expn, '("+term+"\+)', 'i')\." ;
    }else{
     if(firstQuery==true){
                 filterQuery=filterQuery+"fn\:lower-case(\?expn\)='"+term+"' " 
        }else{
                 filterQuery=filterQuery+"|| fn\:lower-case(\?expn\)='"+term+"' " 
        }

        selectQuery=selectQuery+" \?exp excemplify\:hasName \?expn\. ";
    }

  }else if(field=="ExperiTopic"){
      if(to!=-1){
      alert("no OR support for wildcard");
    }else{
      
      
   if(firstQuery==true){
                 filterQuery=filterQuery+"fn\:lower-case(\?topic\)='"+term+"' " 
        }else{
                 filterQuery=filterQuery+"|| fn\:lower-case(\?topic\)='"+term+"' " 
        }
      
         selectQuery=selectQuery+" \?exp excemplify\:hasTopic \?topic\. ";
    }
  }else if((field=="Cells")||(field=="Treatments")||(field=="TimePoints")||(field=="Proteins")||(field=="Antibody")){
       if(type=="Experiments"){ 
      if(to!=-1){
    
       alert("no OR support for wildcard");
     }else{
      
         if(firstQuery==true){
                 filterQuery=filterQuery+"fn\:lower-case(\?v"+queryindex+"\)='"+term+"' " 
        }else{
                 filterQuery=filterQuery+"|| fn\:lower-case(\?v"+queryindex+"\)='"+term+"' " 
        }

         selectQuery=selectQuery+" \?exp excemplify\:hasMetaData \?m"+queryindex+"\. \?m"+queryindex+" excemplify\:isA '"+field+"'\. \?m"+queryindex+" excemplify\:hasValue \?v"+queryindex+"\." ;
    }
       }else if(type=="Data Points") {
               if(to!=-1){
         alert("no OR support for wildcard");
    }else{
      
    if(firstQuery==true){
                 filterQuery=filterQuery+"fn\:lower-case(\?v"+queryindex+"\)='"+term+"' " 
        }else{
                 filterQuery=filterQuery+"|| fn\:lower-case(\?v"+queryindex+"\)='"+term+"' " 
        }
 selectFieldQuery=" \?exp excemplify\:hasData \?d\."
         selectQuery=selectQuery+"\?d excemplify\:has"+field+" \?v"+queryindex+"\." ;
    }
      
       }  
   }else if(field=="Data Value"){
   
     if(type=="Data Points") {
                   if(to!=-1){
         alert("no OR support for wildcard");
    }else{
      
    if(firstQuery==true){
                 filterQuery=filterQuery+"\?dv"+queryindex+"\>\="+dfrom+" \&\& \?dv"+queryindex+"\<\="+dto
        }else{
                filterQuery=filterQuery+"|| \?dv"+queryindex+"\>\="+dfrom+" \&\& \?dv"+queryindex+"\<\="+dto
        }
selectFieldQuery=" \?exp excemplify\:hasData \?d\."
         selectQuery=selectQuery+" \?d excemplify\:hasDataValue \?dv"+queryindex+"\." ;
    }
     }
     
   }
  }
  formQueryString();
  queryindex=queryindex+1
     if(to==-1){
    firstQuery=false; 
     }

}

  function Mail(id,type){
      $("#sendSEEKExpId").html(id); 
        $("#sendType").html(type); 
      $("#email-form").dialog( "open" );
     }

 function warning(){
        $("#warning-form" ).dialog( "open" );
     }

 function warningMessage(str){
         $("#warning-form").html( "<p style='font-family: serif; color:  blue;'>"+str+"</p>");
          $("#warning-form" ).dialog( "open" );
     }

function sendSearchString(){
  var query= $('#sparqlarea').val()

${remoteFunction(controller:'search', action:'sparqlSearch',  params:'\'querystring=\'+encodeURIComponent(query)' ,update:[success:'updateResult',failure:'feedback'], onFailure:'warningMessage(\"not sucess\")') };
    
}


 function showSpinner() {
      $("#spinner").appendTo('#feedback').show();
   }
   function hideSpinner() {
      $("#spinner").hide();
   }

function resetType(){
 type=$('#type').val();
 if(type=="Data Points"){
   $('#sopopt').hide()
   $('#dataopt').show()
 }else if(type=="Experiments"){
    $('#sopopt').show() 
   $('#dataopt').hide() 
 }
}

function checkOption(){
  var optionfield=$('#searchterms').val();
  $('#searchtermField').val("");
    $('#from').val("0");
     $('#to').val("0");
  if(optionfield=="Data Value"){
    $('#searchtermField').hide()
    $('#dataRange').show()
  }else{
       $('#searchtermField').show()
       $('#dataRange').hide()
  }
}

function clearSearchArea(){
  selectQuery="";
  filterQuery="";
  firstQuery=true;
  selectFieldQuery="";
  queryindex=0;
  $('#type').val("Experiments");
    $('#operator').val("AND");
 $('#searchterms').val("ExperimentName");
  $('#sparqlarea').val("");
  checkOption();
  formQueryString();
}
 
     function refreshTableSorter(){
     if ($("#experimentResultList").find("tr").size() > 1){ 
     $("#experimentResultList")
    .tablesorter()
    .tablesorterPager({container: $("#rpager")});
     }

}

    </script>

  <r:layoutResources/>
</head>
<body>

<g:render template="/ui/user/labheader"/>
<g:set var="userObject" value="${User.get(Integer.parseInt(sec.loggedInUserInfo(field:'id').toString()?:'0'))}"/>
<div class="maincontent">
  <h1> Search <small style="color: blue">(only experiments which have been exported once into RDF are searchable here)</small></h1>
  <div id="searchForm">
    Within <select onchange="resetType()"  name="type" id="type" style="width:120px" size="1">
      <option selected="true" value="Experiments">Experiments</option>         
      <option value="Data Points" >Data Points</option> 
    </select>
    <table  id="searchactions">
      <tr>
        <td id="searchreset">

          <textarea id="sparqlarea"  type="text" name="sparqlarea" rows="6" cols="60" ></textarea>
          <input id="searchbtn" type="submit" onclick="sendSearchString()" name="searchgo" value="            " Style ='cursor:pointer'>
          <input id="resetbtn" type="reset" name="resetsearch" onclick="clearSearchArea()" value="           ">


        </td>

        <td id="searchoption" style="vertical-align:top" >
          Form Query String
          <div id="option_expand">
            <br/>
            <input id="addsearch" onclick="addToQueryAndSearch()" type="submit"  name="searchgo" value="">

            <select name="operator" id="operator" size="1">
              <option value="AND" label="AND">AND</option>         
              <option value="OR" label="OR">OR</option> 
            </select>
            <select onchange="checkOption()" name="searchterms" size="1" id="searchterms" >
              <optgroup id="sopopt" label="SOP">
                <option selected="true" value="ExperiName" label="ExperiName">Experiment Name</option>
                <option value="ExperiTopic" label="ExperiTopic">Techniques</option>
              </optgroup>
              <optgroup label="MetaData/Condition">
                <option value="Cells">Cell</option>        
                <option value="TimePoints">Time Point</option>
                <option value="Treatments">Treatment</option>
                <option value="Proteins">Protein</option>
                <option value="Antibody">Antibody</option>
              </optgroup>
              <optgroup id="dataopt" style="display: none" label="Data Point">     
                <option value="Data Value">Data Value</option>
              </optgroup>
            </select>
            <input type="text" name="searchtermField" id="searchtermField"/>
            <div id="dataRange" style="display: none; width:300px">
              <br>
              <label>From:</label><input type="text" name="from" id="from" value="0"/>  <label> To:</label><input  type="text" name="to" id="to" value="0"/>
              <br><p><small style="color: red"> hint: </small> give the same value for both means exact value search</p>
            </div>

          </div>

        </td>
      </tr>
    </table>
  </div>
  <div id="feedback"></div>
  <div id="resultaccordion" >
    <h5><a href="#section1">Search Result (Experiment)</a></h5>
    <div  id="updateResult" style=" max-height: 350px; overflow:  auto" >
      <g:render template="/ui/user/resultexperiment"/>
    </div>
  </div>
  <div id="warning-form" title="Warning">
    <p style="font-family: serif; color:  blue;"> ${flash.message}</p>
  </div>
  <div id="spinner" style="display: none; position: absolute">
    <img src="${createLinkTo(dir: 'images/ui', file: 'spinner.gif')}" alt="Loading..." /> Please waiting .....
  </div>
  <div id="email-form" title="Your email account registered in SEEK VLN">
    <p>Experiment<span id="sendType" hidden="true"></span> <span id="sendSEEKExpId"></span> will be sent to SEEK</p>
    <p class="ui-widget" > Please provide us your registered email address in SEEK virtual liver</p>
    <br>
    <input  name="emailaddress" id="email"  type="text" size="15" value="${User.findById((sec.loggedInUserInfo(field:"id")).toString())?.seekEmailAddress}" />
    <br><p style="color: blue">note: if you do not not want to fill this several times, you can update such information in your user account.</p>  
  </div>
</div>
<r:layoutResources/>
<g:applyLayout name="foot">
</g:applyLayout>
</body>
</html>
