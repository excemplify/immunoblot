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


    <!--Other Required jQuery UI Widget-->
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.ui.core.js')}"></script>
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.ui.widget.js')}"></script>
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.ui.accordion.js')}"> </script>
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.ui.dialog.js')}"></script>
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.ui.mouse.js')}"></script>
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.ui.position.js')}"> </script>
    <script type="text/javascript" src="${createLinkTo(dir:'js', file:'jquery.ui.resizable.js')}"></script>
    <!--Other Required jQuery UI Widget-->

    <script src="${createLinkTo(dir:'js/TogetherJS', file:'togetherjs-min.js')}"></script>


    <title>Excemplify</title>
    <link rel="shortcut icon" href="${createLinkTo(dir: 'images/ui/', file: 'logo.ico')}" type="image/x-icon"/>
    <script type="text/javascript">

     window.onload = function(){
      var active=0;
           if("${session.getAttribute('active')}"==""){  
           active=0;
           }else{
             active=parseInt("${session.getAttribute('active')}");
           }
           
           
        $( "#accordion" ).accordion({
                        collapsible: true,
                        autoHeight:true,
                        active:active
                });
      
  
        $( "#dialog:ui-dialog" ).dialog( "destroy" );
        $("#profile-form").dialog({
                      autoOpen: false,
                        height:350,
                        width: 600,
                        modal: true,
                        buttons:{
                           "Update User Profile": function() {
                        var currentuserid=$("#curuserid").val(); 
                        var currentusername=$("#curusername").val(); 
                        var currentuserpwd=$("#curuserpwd").val();  
                        var currentemail=$("#curemail").val();
${remoteFunction(controller:'user', action:'updateProfile', params:'\'id=\'+currentuserid+\'&username=\'+currentusername+\'&password=\'+currentuserpwd+\'&seekEmailAddress=\'+currentemail', update:[success:'profile-form',failure:'resource'], onFailure:'warning()', onLoading:'showSpinner()', onComplete:'hideSpinner()') };       
                             $( this ).dialog( "close" );
                                
                                }, //
                          Cancel: function() {
              
                              $( this ).dialog( "close" );
                                        
                                }
                        },
                       close: function() {
                                allFields.val( "" ).removeClass( "ui-state-error" );
                        }
        });
        
        $("#warning-form").dialog({
                      autoOpen: false,
                        height: 400,
                        width: 400,
                        modal: true,
                        buttons:{
                           "Close": function() {
                              $("#resource").html("");
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
${remoteFunction(controller:'experiment', action:'mailToSeek', params:'\'id=\'+id+\'&email=\'+emailaddress+\'&type=\'+type', update:[success:'resource', failure:'resource'], onFailure:'warning()', onLoading:'showSpinner()', onComplete:'hideSpinner()') };
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
        
                   $("#confirm-dialog").dialog({
                      autoOpen: false,
                        height: 200,
                        width: 300,
                        modal: true,
                        buttons:{
                          "Yes, delete it": function() {
                             var id= $("#deleteExpId").html(); 
              
                             if(id.length>0){
${remoteFunction(controller:'experiment', action:'deleteExperiment',  params:'\'id=\'+id' ,update:[success:'updateMe',failure:'resource'], onFailure:'warning()') };
      }                                  
                                        $( this ).dialog( "close" );
                                          $("#deleteExpName").html(""); 
                                          $("#deleteExpId").html(""); 
                                

                                }, //
                          Cancel: function() {
              
                              $( this ).dialog( "close" );
                                        
                                }
                        },
                       close: function() {
                                allFields.val( "" ).removeClass( "ui-state-error" );
                        }
        });

            $("#pconfirm-dialog").dialog({
                      autoOpen: false,
                        height: 200,
                        width: 300,
                        modal: true,
                        buttons:{
                          "Yes, delete it": function() {
                     
                             var pid= $("#deletePExpId").html(); 
              if(pid.length>0){
${remoteFunction(controller:'experiment', action:'deleteExperiment',  params:'\'id=\'+pid' ,update:[success:'updatePMe',failure:'resource'], onFailure:'warning()') };
   
      }                                   
                                        $( this ).dialog( "close" );
                                          $("#deletePExpName").html(""); 
                 
                                          $("#deletePExpId").html(""); 

                                }, //
                          Cancel: function() {
              
                              $( this ).dialog( "close" );
                                        
                                }
                        },
                       close: function() {
                                allFields.val( "" ).removeClass( "ui-state-error" );
                        }
        });
       	$( "#dialog-form" ).dialog({
                        autoOpen: false,
                        height: 760,
                        width: 650,
                        modal: true,
                        buttons: {
                                "Create an experiment": function() {
                                  var experimentName= $("#expname1").val();
                                  var experimentTopic=$("#topic1").val();
                                  var setuptemplate=$("#template").val();
                                  var loadingtemplate="default";
                                  var rawdatatemplate="default";
                                  var gelinspectortemplate="default";
//                                  var loadingtemplate=$("#templateloading").val();
//                                  var rawdatatemplate=$("#templaterawdata").val();
//                                  var gelinspectortemplate=$("#templategelinspector").val();
                                 // alert(experimentName+":"+experimentTopic+":"+setuptemplate+":"+loadingtemplate+":"+rawdatatemplate+":"+gelInspectortemplate)
                                  var setUpResourceId="${session.getAttribute('setUpResourceId')}";
                                 // var stimulus
                                 var blotnum=$("#blotnum").val();
                                  var randomization
                                  var min1=$("#minDis1").val();
                                  var min2=$("#minDis2").val();
                                  var min3=$("#minDis3").val();
//                                  if($('input[name=attribute1]').is(':checked')){
//                                    stimulus=true
//                                  }else{
//                                    stimulus=false
//                                  }
                                   if($('input[name=attribute2]').is(':checked')){
                                    randomization=true
                                  }else{
                                    randomization=false
                                  }
                                 // alert('\'experimentName=\'+experimentName+\'&experimentTopic=\'+experimentTopic+\'&setuptemplate=\'+setuptemplate+\'&loadingtemplate=\'+loadingtemplate+\'&rawdatatemplate=\'+rawdatatemplate+\'&gelinspectortemplate=\'+gelinspectortemplate+\'&setUpResourceId=\'+setUpResourceId+\'&stimulus=\'+stimulus+\'&randomization=\'+randomization+\'&min1=\'+min1+\'&min2=\'+min2+\'&min3=\'+min3');
${remoteFunction(controller:'lab', action:'initialExp', params:'\'experimentName=\'+experimentName+\'&experimentTopic=\'+experimentTopic+\'&setuptemplate=\'+setuptemplate+\'&loadingtemplate=\'+loadingtemplate+\'&rawdatatemplate=\'+rawdatatemplate+\'&gelinspectortemplate=\'+gelinspectortemplate+\'&setUpResourceId=\'+setUpResourceId+\'&blotnum=\'+blotnum+\'&randomization=\'+randomization+\'&min1=\'+min1+\'&min2=\'+min2+\'&min3=\'+min3', update:'updateMe', onLoading:'showSpinner()', onComplete:'hideSpinner()') };
                                  $( this ).dialog( "close" );

                                }, //end create experiment

                                Cancel: function() {
${remoteFunction(controller:'lab', action:'clear', params:'\'after=\'+1', update:'resource', onLoading:'showSpinner()', onComplete:'hideSpinner()') };
                          
                                        $( this ).dialog( "close" );
                                        afterUpload(1);
                                       
                                }
                        },
                        close: function() {
                                allFields.val( "" ).removeClass( "ui-state-error" );
                                 
                        }
                });
                
       	$( "#performed-dialog-form" ).dialog({
                        autoOpen: false,
                        height:760,
                        width: 650,
                        modal: true,
                        buttons: {
                                "Backing up performed experiment": function() {
                                 var experimentName= $("#expname3").val();
                                  var experimentTopic=$("#topic3").val();
                                  var setuptemplate=$("#template3").val();
                                   var loadingtemplate="default";
                                  var rawdatatemplate="default";
                                  var gelinspectortemplate="default";
//                                  
//                                    var loadingtemplate=$("#templateloading3").val();
//                                  var rawdatatemplate=$("#templaterawdata3").val();
//                                   var gelinspectortemplate=$("#templategelinspector3").val();
                                  //var geltemplate=$("#template4").val();
                                  var setUpResourceId="${session.getAttribute('performedSetUpResourceId')}";

${remoteFunction(controller:'lab', action:'initialPerExp', params:'\'experimentName=\'+experimentName+\'&experimentTopic=\'+experimentTopic+\'&setuptemplate=\'+setuptemplate+\'&loadingtemplate=\'+loadingtemplate+\'&rawdatatemplate=\'+rawdatatemplate+\'&gelinspectortemplate=\'+gelinspectortemplate+\'&setUpResourceId=\'+setUpResourceId', update:'updatePMe', onLoading:'showSpinner()', onComplete:'hideSpinner()') };
                                        
                          $( this ).dialog( "close" );

                                }, //end update experiment

                                Cancel: function() {
${remoteFunction(controller:'lab', action:'clear', params:'\'after=\'+3', update:'resource', onLoading:'showSpinner()', onComplete:'hideSpinner()') };
                                        $( this ).dialog( "close" );
                                        afterUpload(3);
                                       
                                }
                        },
                        close: function() {
                                allFields.val( "" ).removeClass( "ui-state-error" );
                                 
                        }
                });
                

       	$( "#dialog-form-update" ).dialog({
                        autoOpen: false,
                        height:760,
                        width: 650,
                        modal: true,
                        buttons: {
                                
                                "Update Experiment": function() {
                                  var experimentId="${session.getAttribute('experimentId')}";                          
                                  var experimentTopic=$("#topic2").val();
                                  var setuptemplate=$("#template2").val();     
                                   var loadingtemplate="default";
                                  var rawdatatemplate="default";
                                  var gelinspectortemplate="default";
                                  var setUpResourceId="${session.getAttribute('setUpResourceIdUpdate')}";                     
                                //  var stimulus
                                var blotnum=$("#blotnumupdate").val();
                                  var randomization
                                  var min1=$("#minDis1update").val();
                                  var min2=$("#minDis2update").val();
                                  var min3=$("#minDis3update").val();
                                   if($('input[name=attribute4]').is(':checked')){
                                    randomization=true
                                  }else{
                                    randomization=false
                                  }
${remoteFunction(controller:'lab', action:'updateExp', params:'\'experimentName=\'+experimentName+\'&experimentId=\'+experimentId+\'&experimentTopic=\'+experimentTopic+\'&setuptemplate=\'+setuptemplate+\'&loadingtemplate=\'+loadingtemplate+\'&rawdatatemplate=\'+rawdatatemplate+\'&gelinspectortemplate=\'+gelinspectortemplate+\'&setUpResourceId=\'+setUpResourceId+\'&blotnum=\'+blotnum+\'&randomization=\'+randomization+\'&min1=\'+min1+\'&min2=\'+min2+\'&min3=\'+min3', update:'updateMe', onLoading:'showSpinner()', onComplete:'hideSpinner()') };
                                  $( this ).dialog( "close" );

                                }, //end update experiment

                                Cancel: function() {
${remoteFunction(controller:'lab', action:'clear', params:'\'after=\'+2', update:'resource', onLoading:'showSpinner()', onComplete:'hideSpinner()') };     
                                        $( this ).dialog( "close" );
                                        afterUpload(2);
                                       
                                }
                        },
                        close: function() {
                                allFields.val( "" ).removeClass( "ui-state-error" );
                                 
                        }
                });
                

    
        $('select#template').selectmenu({style:'popup'});
        $('select#template2').selectmenu({style:'popup'});
       $('select#template3').selectmenu({style:'popup'});

          $('select#blotnum').selectmenu({style:'popup'});
        $('select#minDis1').selectmenu({style:'popup'});
        $('select#minDis2').selectmenu({style:'popup'});
        $('select#minDis3').selectmenu({style:'popup'});
                $('select#blotnumupdate').selectmenu({style:'popup'});
        $('select#minDis1update').selectmenu({style:'popup'});
        $('select#minDis2update').selectmenu({style:'popup'});
        $('select#minDis3update').selectmenu({style:'popup'});
       var experimentName="${session.getAttribute('experimentName')}";

         if(experimentName!=""){
             $("#expname2").val(experimentName)
         }
         var setUp="${session.getAttribute('setUpResourceName')}";
          if(setUp!=""){
             $("#up1").html("<p><img src='${createLinkTo(dir: 'images/ui/', file: 'ready.gif')}' alt='set up uploaded'>  "+setUp+" uploaded</p>");
             $("#mySetUpUploadTd1").html("");
             $("#dialog-form" ).dialog( "open" );          
            }
         var setUpUpdate="${session.getAttribute('setUpResourceNameUpdate')}";
          if(setUpUpdate!=""){
             $("#up2").html("<p><img src='${createLinkTo(dir: 'images/ui/', file: 'ready.gif')}' alt='set up uploaded'>  "+setUpUpdate+" uploaded</p>");
             $("#mySetUpUploadTd2").html("");
             $("#dialog-form-update" ).dialog( "open" );          
            }
            var performSetUp="${session.getAttribute('performedSetUpResourceName')}";

            
               if(performSetUp!=""){
             $("#up3").html("<p><img src='${createLinkTo(dir: 'images/ui/', file: 'ready.gif')}' alt='set up uploaded'>  "+performSetUp+" uploaded</p>");
               $("#mySetUpUploadTd3").html("");
            $("#performed-dialog-form" ).dialog( "open" );          
            }
            
       var select="${session.getAttribute('selector')}";
         if(select!=""){
           activeButton(select);
             }
     refreshTableSorter();

     //test configure togetherJS
    TogetherJSConfig_getUserName = function () {return "${User.get(Integer.parseInt(sec.loggedInUserInfo(field:'id').toString()?:'0'))}";};
    TogetherJSConfig_on_ready = function () {sendTogetherJSURLToServer(TogetherJS.shareUrl());};  //call TogetgerJS.shareUrl() can get the session link

    
     }//end load
     
     //customized togetherJS
      
     
     
     
     function sectionSelection(nr){
     $("#accordion").accordion({ active: nr}); //works
 
     }
     function resetEmail(val){
         $("#email").val(val); 
     }
     
     function activeButton(selector){
       if(selector=="new"){
                $("#dialog-form").parent().find("button:contains('Create an experiment')").attr('disabled', false);
       }else if(selector=="update"){
           $("#dialog-form-update").parent().find("button:contains('Update an experiment')").attr('disabled', false);
       }else if(selector=="performed"){
            $("#performed-dialog-form").parent().find("button:contains('Backing up performed experiment')").attr('disabled', false);
       }
       
     }
 function showSpinner() {
      $("#spinner").appendTo('#waitingPlace').show();
   }
   function hideSpinner() {
      $("#spinner").hide();
   }

     
     function showUpdateDialog(expid,expname){
     $("#up2").html("<form id='upFormUpdate' action='/excemplify/lab/updateupload/upFormUpdate?experimentName="+expname+"&experimentId="+expid+"'  enctype='multipart/form-data' method='POST'><td><label for='mySetUpUpdate'><b>Update Set Up File:</b></label><input name='mySetUpUpdate' id='mySetUpUpdate'  type='file'/></td><td id='mySetUpUploadTd2'  style='text-align:right' ><input class='ui-button ui-state-default ui-widget ui-corner-all ui-button-text-only' type='submit' name='submit' value='Upload' /></td></form>")
      $("#expname2").val(expname)
      $("#dialog-form-update" ).dialog( "open" );
      $("#dialog-form-update").parent().find("button:contains('Update an experiment')").attr('disabled', true);
     }
 
     function refreshTableSorter(){
     if ($("#templateList").find("tr").size() > 1){ 
     $("#templateList")
    .tablesorter();
     }
     if ($("#experimentList").find("tr").size() > 1){
     $("#experimentList")
    .tablesorter()
    .tablesorterPager({container: $("#pager")});
    }
     if ($("#experimentList2").find("tr").size() > 1){
      $("#experimentList2")
    .tablesorter()
    .tablesorterPager({container: $("#ppager")});
     }
}
  
       
        function afterUpload(nr, nnr){
        $("#up"+nr).html($("#hidden"+nr).html());
        $("#up"+nnr).html($("#hidden"+nnr).html());
        $("#mySetUpUploadTd"+nr).html($("#hiddensubmit").html());
        $("#expname"+nr).val("");
       }
//         function afterUpload2(){
//        $("#up2").html($("#hidden2").html());
//        $("#expname2").val("");
//       }
       
     function initialPerformedExperiment(){
            $("#performed-dialog-form" ).dialog( "open" );
          
       $("#performed-dialog-form").parent().find("button:contains('Backing up performed experiment')").attr('disabled', true);
     }  
     function initialExperiment(){
          $("#dialog-form").parent().find("button:contains('Create an experiment')").attr('disabled', true);
     
     $("#dialog-form" ).dialog( "open" );
     
     }

     function chooseLayout(){
      $("#layout-form" ).dialog( "open" );
     }

//     function uploadRawData(id){
//       /**
//        * ./lab/uploadr?experimentId='+id,'Uploading'
//        */
//       str="./lab/uploadr?experimentType=new&&resourceType=rawdata&&experimentId="+id
//
//     document.location.href =str
//     }
       function uploadData(type, rtype,id){
       /**
        * ./lab/uploadr?experimentId='+id,'Uploading'
        */
       str="./lab/uploadr?experimentType="+type+"&&resourceType="+rtype+"&&experimentId="+id

     document.location.href =str
     }

     function warning(){
        $("#warning-form" ).dialog( "open" );
     }
     function warningMessage(str){
         $("#warning-form").html( "<p style='font-family: serif; color:  blue;'>"+str+"</p>");
          $("#warning-form" ).dialog( "open" );
     }
     function updateProfile(){
       
          $("#profile-form" ).dialog( "open" ); 
     }
     function Mail(id,type){
      $("#sendSEEKExpId").html(id); 
        $("#sendType").html(type); 
      $("#email-form").dialog( "open" );
     }

     function DeleteExp(id, name){
        $("#deleteExpName").html(name); 
         $("#deleteExpId").html(id); 
  $("#confirm-dialog" ).dialog( "open" );
     }
      function DeletePExp(id, name){
        $("#deletePExpName").html(name); 
         $("#deletePExpId").html(id); 
  $("#pconfirm-dialog" ).dialog( "open" );
     }
      function ShareExp(id){

${remoteFunction(controller:'experiment', action:'shareExperiment',  params:'\'id=\'+id' ,update:[success:'updateMe',failure:'resource'], onFailure:'warningMessage(\"not sucess\")') };
 }
      
    
    function SharePExp(id){
${remoteFunction(controller:'experiment', action:'shareExperiment',  params:'\'id=\'+id' ,update:[success:'updatePMe',failure:'resource'], onFailure:'warningMessage(\"not sucess\")') };
       }
       
      function StopShareExp(id){

${remoteFunction(controller:'experiment', action:'stopShareExperiment',  params:'\'id=\'+id' ,update:[success:'updateMe',failure:'resource'], onFailure:'warningMessage(\"not sucess\")') };


   }
   function StopSharePExp(id){

${remoteFunction(controller:'experiment', action:'stopShareExperiment',  params:'\'id=\'+id' ,update:[success:'updatePMe',failure:'resource'], onFailure:'warningMessage(\"not sucess\")') };
  
   }  
     

     

    </script>

  <r:layoutResources/>

</head>
<body>

<g:render template="/ui/user/labheader"/>
<g:set var="userObject" value="${User.get(Integer.parseInt(sec.loggedInUserInfo(field:'id').toString()?:'0'))}"/>
<div class="maincontent">
  <div class="buttoncontent">
    <span class="navspan" onclick="initialExperiment()">  <img alt="New Experiment" src="${createLinkTo(dir: 'images/ui', file: 'plus.png')}" title="For Your New Experiment" />New Experiment
    </span>
    <span class="navspan" onclick="initialPerformedExperiment()">  <img alt="Performed Experiment" src="${createLinkTo(dir: 'images/ui', file: 'plus.png')}" title="For Your Performed (Old) Experiment" />Performed Experiment
    </span>
    <span class="navspan" > <p id="waitingPlace"></p></span>
<!--     <span class="navspan"> <g:link controller="search" class="menuButton" action="sparqlQuery"><img alt="Go To Search Page" src="${createLinkTo(dir: 'images/ui', file: 'search.png')}" title="Go To Search Page" />Go To Search Page</g:link>
  </span>-->
    <span class="navspan"><a href="${createLink(uri:'/lab/search')}" ><img alt="Go To Search Page" src="${createLinkTo(dir: 'images/ui', file: 'search.png')}" title="Go To Search Page" />Go To Search Page</a>
    </span>
  </div>
<!--  <button onclick="TogetherJS(this);return false;">Start Discuss</button>
<button onclick="alert(TogetherJS.shareUrl());">Share Url</button>-->
  <div id="resource" class="messagecontent">
    <g:if test="${flash.message}">
      <P style="float:right;" > oops~ <img onclick="warning()" style=" cursor: pointer"  src="${createLinkTo(dir:'images/ui', file:'warning.png')}" alt="warning" >
      </P> 
    </g:if>
  </div>

  <div id="accordion" class="corecontent" >
    <h5><a href="#section1">Stored New Experiments </a></h5>
    <div  id="updateMe" style=" max-height: 350px; overflow:  auto" >
      <g:render template="/ui/user/experiment" model="['experimentInstanceList': Experiment?.findAllByAuthor(userObject).findAll{it.type=='new'}]"/>
    </div>
    <h5><a href="#section2">Stored Performed Experiments </a></h5>
    <div  id="updatePMe" style=" max-height: 350px; overflow:  auto" >
      <g:render template="/ui/user/performedexperiment" model="['experimentPInstanceList': Experiment?.findAllByAuthor(userObject).findAll{it.type=='performed'}]"/>
    </div>
    <h5><a href="#section3">Template Library</a></h5>
    <div id="updateTemplate" style=" max-height: 350px; overflow:  auto" >

      <g:render template="/ui/user/template" model="['templateInstanceList': Template?.findAll('from Template as b where b.type =:templateType and b.visible=:visibility',[templateType:'public', visibility:true]), 'templateInstanceTotal': Template?.findAll('from Template as b where b.type =:templateType and b.visible=:visibility',[templateType:'public', visibility:true]).size(), params:params]"/>
    </div>
  </div>
  <div class="hiddencontent">
    <div id="dialog-form" title="Initialize A New Experiment" >

      <table style=" width:550px; border-collapse: separate; border-spacing: 10px;" class="ui-widget" >
        <tr><td><b>Step 1:</b></td></tr>
        <g:form id="upForm" controller="lab" action="upload" enctype="multipart/form-data"  method="POST">
          <tr style=" width:550px; "><td id="up1">
              <label for="mySetUp" style="width: 200px"><b>Set Up File:</b></label>
              <input name="mySetUp" id="mySetUp"  type="file" style="width: 350px"/>
            </td>
          </tr>
          <tr><td id="mySetUpUploadTd1"  style=" text-align:  right" ><input class="ui-button ui-state-default ui-widget ui-corner-all ui-button-text-only" type="submit" name="submit" value="Upload" /></td></tr>
        </g:form>
        <tr><td><hr size="3" width="100%" noshade   align="right"></td></tr>  

        <tr><td><b>Step 2:</b></td></tr>
        <tr  style=" width:550px; "><td><label for="expname1"><b>Experiment Name: </b></label> <input  style=" width:250px; " type="text" name="expname1" id="expname1" class="text ui-widget-content ui-corner-all" /></td></tr>
        <tr  style=" width:550px; "><td><label for="topic1"><b>Experiment Type: </b></label>
            <select name="topic1" id="topic1" class="text ui-widget-content ui-corner-all"  style=" width:250px; " >
              <option selected="true" value="Immunoblot">Immunoblot</option>
              <option value="FACS">FACS</option>
              <option value="Protein Array">Protein Array</option>
              <option value="PCR">PCR</option>
              <option value="ELISA">ELISA</option>
              <option value="Luminex">Luminex</option>
            </select> 
          </td>
        </tr>
        <tr><td>

        <g:render template="/ui/user/templateSelec" model="['templateInstanceList': Template?.findAll('from Template as b where b.type =:templateType and b.purpose=:purpose and b.visible=:visibility order by b.id desc', [templateType:'public', purpose:'setup', visibility:true])]"/>
        <br></td>
        </tr>
        <tr><td style="border: 1px; border-color: blue; border-style:dashed"><b>Blot Numbers:</b>
            <select name="blotnum" style=" width:50px;" id="blotnum">
              <option  value="1">1</option>
              <option  value="2">2</option>
              <option selected="true" value="3">3</option>
              <option   value="4">4</option>
              <option  value="5">5</option>
              <option  value="6">6</option>
              <option  value="7">7</option>
              <option  value="8">8</option>
            </select>
          </td>
        </tr>
        <tr><td ><label for="attribute"><b>Properties: </b></label></td></tr>
<!--        <tr><td >
            <input class="checkbox" name="attribute1" type="checkbox" value="stimulus">Stimulus (+/-)
          </td>
        </tr>-->
        <tr><td style="border: 1px; border-color: blue; border-style:dashed">
            <input class="checkbox" name="attribute2" type="checkbox" checked="checked"  value="random">Randomization
            <table style="width: 100%">
              <tr><td >Minimal slot distance between sample(i, i+1): 
                  <select name="minDis1" style=" width:50px;" id="minDis1">
                    <option value="1">1</option>
                    <option   value="2">2</option>
                    <option selected="true" value="3">3</option>
                  </select>
                </td></tr>
              <tr><td>Minimal slot distance between sample(i, i+2): 
                  <select name="minDis2" style=" width:50px;" id="minDis2">
                    <option value="1">1</option>
                    <option  selected="true"  value="2">2</option>
                    <option value="3">3</option>
                  </select></td></tr>
              <tr><td>Minimal slot distance between sample(i, i+3):
                  <select name="minDis3" style=" width:50px;" id="minDis3">
                    <option  selected="true" value="1">1</option>
                    <option  value="2">2</option>
                    <option value="3">3</option>
                  </select>
                </td></tr>
            </table>
          </td></tr>
      </table>


    </div>


    <div id="performed-dialog-form" title="Backing A Performed Experiment" >

      <table style=" width:550px; border-collapse: separate; border-spacing: 10px;" class="ui-widget" >
        <tr><td><b>Step 1:</b></td></tr>
        <g:form id="upFormPerformed" controller="lab" action="uploadperformed" enctype="multipart/form-data"  method="POST">
          <tr style=" width:550px; "><td id="up3">
              <label for="myPerformedSetUp" style="width: 200px"><b>Set Up File:</b></label>
              <input name="myPerformedSetUp" id="myPerformedSetUp"  type="file"  style="width: 350px" />
            </td>

          <tr><td id="mySetUpUploadTd3"  style=" text-align:  right" > <input class="ui-button ui-state-default ui-widget ui-corner-all ui-button-text-only" type="submit" name="submit" value="Upload" /></td></tr>
          <tr><td><hr size="3" width="100%" noshade   align="right"></td></tr>
        </g:form>
        <tr><td><b>Step 2:</b></td></tr>
        <tr  style=" width:550px; "><td><label for="expname3"><b>Experiment Name: </b></label> <input  style=" width:250px; " type="text" name="expname3" id="expname3" class="text ui-widget-content ui-corner-all" /></td></tr>
        <tr  style=" width:550px; "><td><label for="topic3"><b>Experiment Type: </b></label>
            <select name="topic3" id="topic3" class="text ui-widget-content ui-corner-all"  style=" width:250px; " >
              <option selected="true" value="Immunoblot">Immunoblot</option>
              <option value="FACS">FACS</option>
              <option value="Protein Array">Protein Array</option>
              <option value="PCR">PCR</option>
              <option value="ELISA">ELISA</option>
              <option value="Luminex">Luminex</option>
            </select> 
          </td>
        </tr>
        <tr>
        <g:render template="/ui/user/templateSelec3"  model="['templateInstanceList': Template?.findAll('from Template as b where b.type =:templateType and b.purpose=:purpose and b.visible=:visibility order by b.id desc', [templateType:'public', purpose:'setup', visibility:true])]"/>
        <br>
        </tr>

<!--      <tr  style=" width:550px; "><td><label for="template4"><b>Using GelInspector Template: </b></label>
 </td></tr>-->

      </table>


    </div>
    <div id="dialog-form-update" title="Update Set Up File" >

      <table style=" width:550px; border-collapse: separate; border-spacing: 10px;" class="ui-widget" >
        <tr><td><b>Step 1:</b></td></tr><tr><td id="up2">
        <g:form id="upFormUpdate" controller="lab" action="updateupload"  enctype="multipart/form-data" method="POST">
          <label for="mySetUpUpdate"><b>Update Set Up File:</b></label>
          <input name="mySetUpUpdate" id="mySetUpUpdate"  type="file" /> 
          <input id="mySetUpUploadTd2"  class="ui-button ui-state-default ui-widget ui-corner-all ui-button-text-only" type="submit" name="submit" value="Upload" />
        </g:form></td>
        </tr>
        <tr><td><hr size="3" width="100%" noshade   align="right"></td></tr>
        <tr><td><b>Step 2:</b></td></tr>

        <tr  style=" width:550px; "><td><label for="expname2"><b>Experiment Name: </b></label> <input  style=" width:250px; color: grey " disabled="true" type="text" name="expname2" id="expname2" class="text ui-widget-content ui-corner-all" /></td></tr>
        <tr  style=" width:550px; "><td><label for="topic2"><b>Experiment Type: </b></label>
            <select name="topic2" id="topic2" class="text ui-widget-content ui-corner-all"  style=" width:250px; " >
              <option selected="true"  value="Immunoblot">Immunoblot</option>
              <option value="FACS">FACS</option>
              <option value="Protein Array">Protein Array</option>
              <option value="PCR">PCR</option>
              <option value="ELISA">ELISA</option>
              <option value="Luminex">Luminex</option>
            </select> 
          </td>
        </tr>
        <tr>
        <g:render template="/ui/user/templateSelec2"  model="['templateInstanceList': Template?.findAll('from Template as b where b.type =:templateType and b.purpose=:purpose and b.visible=:visibility order by b.id desc', [templateType:'public', purpose:'setup', visibility:true])]"/>
        <br>
        </tr>   
        <tr><td style="border: 1px; border-color: blue; border-style:dashed"><b>Blot Numbers:</b>
            <select name="blotnumupdate" style=" width:50px;" id="blotnumupdate">
              <option  value="1">1</option>
              <option  value="2">2</option>
              <option selected="true" value="3">3</option>
              <option   value="4">4</option>
              <option  value="5">5</option>
              <option  value="6">6</option>
              <option  value="7">7</option>
              <option  value="8">8</option>
            </select>
          </td>
        </tr>
        <tr><td><label for="attribute"><b>Properties: </b></label></td></tr>
        <tr><td style="border: 1px; border-color: blue; border-style:dashed">
            <input class="checkbox" name="attribute4" type="checkbox" checked="checked"  value="random">Randomization
          </td></tr>
        <tr><td>
            <table style="width: 100%">
              <tr><td >Minimal slot distance between sample(i, i+1): 
                  <select name="minDis1update" style=" width:50px;" id="minDis1update">
                    <option  value="1">1</option>
                    <option  value="2">2</option>
                    <option selected="true" value="3">3</option>
                  </select>
                </td></tr>
              <tr><td>Minimal slot distance between sample(i, i+2): 
                  <select name="minDis2update" style=" width:50px;" id="minDis2update">
                    <option value="1">1</option>
                    <option  selected="true"  value="2">2</option>
                    <option value="3">3</option>
                  </select></td></tr>
              <tr><td>Minimal slot distance between sample(i, i+3):
                  <select name="minDis3update" style=" width:50px;" id="minDis3update">
                    <option selected="true"  value="1">1</option>
                    <option  value="2">2</option>
                    <option value="3">3</option>
                  </select>
                </td></tr>
            </table>
          </td>
        </tr>
      </table>
    </div>



    <div id="spinner" style="display: none; position: absolute">
      <img src="${createLinkTo(dir: 'images/ui', file: 'spinner.gif')}" alt="Loading..." /> Please waiting .....
    </div>
    <div id="warning-form" title="Warning">
      <p style="font-family: serif; color:  blue;"> ${flash.message}</p>
    </div>

    <div id="email-form" title="Your email account registered in SEEK VLN">
      <p>Experiment<span id="sendType" hidden="true"></span> <span id="sendSEEKExpId"></span> will be sent to SEEK</p>
      <p class="ui-widget" > Please provide us your registered email address in SEEK virtual liver</p>
      <br>
      <input  name="emailaddress" id="email"  type="text" size="15" value="${User.findById((sec.loggedInUserInfo(field:"id")).toString())?.seekEmailAddress}" />
      <br><p style="color: blue">note: if you do not not want to fill this several times, you can update such information in your user account.</p>  
    </div>
    <div id="profile-form" title="Your Current User Profile">
      <g:render template="/ui/user/userProfile" model="['userInstance':User.findById((sec.loggedInUserInfo(field:'id')).toString())]"/>
    </div>

    <div id="confirm-dialog" title="Are you sure" >
      <p class="ui-widget" > Are you sure you want to delete the whole <span style="color:blue" id="deleteExpName"></span><span style="display: none" id="deleteExpId"></span>??</p>
    </div>


    <div id="pconfirm-dialog" title="Are you sure" >
      <p class="ui-widget" > Are you sure you want to delete the whole <span style="color:blue" id="deletePExpName"></span><span style="display: none" id="deletePExpId"></span>??</p>
    </div>
    <div style="display: none " id="hidden1" >
      <label for="mySetUp" style="width: 200px"><b>Set Up File:</b></label>
      <input name="mySetUp" id="mySetUp"  type="file" style="width: 350px"/>

    </div>
    <div style="display: none " id="hidden2" >
      <label for="mySetUpUpdate" style="width: 200px"><b>Update Set Up File:</b></label>
      <input name="mySetUpUpdate" id="mySetUpUpdate"  type="file" style="width: 350px"/>
    </div>

    <div style="display: none " id="hidden3" >
      <label for="myPerformedSetUp" style="width: 200px"><b>Set Up File:</b></label>
      <input name="myPerformedSetUp" id="myPerformedSetUp"  type="file"  style="width: 350px" />
    </div>
    <div style="display: none " id="hidden4" >
      <label for="myPerformedSetUp2" style="width: 200px"><b>GelInspector Blot1:</b></label>
      <input name="myPerformedSetUp2" id="myPerformedSetUp2"  type="file"  style="width: 350px"/>     
      <label for="myPerformedSetUp3" style="width: 200px"><b>GelInspector Blot2:</b></label>
      <input name="myPerformedSetUp3" id="myPerformedSetUp3"  type="file"  style="width: 350px"/>    
      <label for="myPerformedSetUp4" style="width: 200px"><b>GelInspector Blot3:</b></label>
      <input name="myPerformedSetUp4" id="myPerformedSetUp4"  type="file"  style="width: 350px"/>     
    </div>

    <div  style="display: none " id="hiddensubmit"> <input class="ui-button ui-state-default ui-widget ui-corner-all ui-button-text-only"  type="submit" name="submit" value="Upload" /></div>
  </div>
</div>
<g:applyLayout name="foot">
</g:applyLayout>

<r:layoutResources/>
</body>
</html>
