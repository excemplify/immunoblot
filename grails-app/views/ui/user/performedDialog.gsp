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
--}%<!--
  To change this template, choose Tools | Templates
  and open the template in the editor.
-->

<%@ page contentType="text/html;charset=UTF-8" %>

<div id="performed-dialog-form" title="Backing A Performed Experiment" >

    <table style=" width:450px; border-collapse: separate; border-spacing: 10px;" class="ui-widget" >

      <tr style=" width:450px; "><td id="up3">
      <g:form id="upFormPerformed" controller="lab" action="upload" enctype="multipart/form-data"  method="POST">
        <label for="myPerformedSetUp"><b>Set Up File:</b></label>
        <input name="myPerformedSetUp" id="myPerformedSetUp"  type="file" />
        <input id="myPerformedSetUpUpload"  type="submit" name="submit" value="Upload" />
      </g:form>
      </td></tr>
      <tr  style=" width:450px; "><td><label for="expname3"><b>Experiment Name: </b></label> <input  style=" width:250px; " type="text" name="expname3" id="expname3" class="text ui-widget-content ui-corner-all" /></td></tr>
      <tr  style=" width:450px; "><td><label for="template3"><b>Using Set Up Template: </b></label>
      <g:render template="/ui/user/templateSelec3" model="['templateInstanceList': Template?.findAll('from Template as b where b.type =:templateType and b.purpose=:purpose', [templateType:'public', purpose:'setup'])]"/>
      </td></tr>
      
    </table>


  </div>
