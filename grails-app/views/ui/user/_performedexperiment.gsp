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
<!--
  To change this template, choose Tools | Templates
  and open the template in the editor.
-->

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.text.SimpleDateFormat" %>



<html>


  <body>


    <div class="body"  style="height:350px; width:100%" >
      <div  style="max-height: 350px; overflow: auto  ">
        <table id="experimentList2" class="tablesorter">
          <thead>
            <tr>             
              <th>Experiment (zip)</th>
              <th>Type</th>
              <th>Created At</th>
<!--              <th>Workbook</th>-->
              <th>Rawdata Files</th>
              <th>Orig. GelInspector(Upload/Export)</th>
              <th>Other Files<img style="cursor: pointer" onclick="warningMessage('Here you can upload other non excel files. e.g. image file, pdf file');" src="${createLinkTo(dir:'images/ui', file:'attention.png')}" alt="other related files" ></th>
              <th>Update details (log)</th>
              <th>State(share/private it)</th>
              <th>Delete</th>
              <th>Send To SEEK ??</th>
            </tr>
          </thead>
          <tbody>

          <g:each  in="${experimentPInstanceList}" status="i" var="experimentPInstance">

            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
              <td style=" white-space:pre-line; text-align: justify"><b>${i+1}. </b>${fieldValue(bean: experimentPInstance, field: "filename")}<g:if test="${experimentPInstance.resources.findAll{(it.type=='rawdata'||it.type=="gelinspector")&& it.state=='active'}.size()>0}"><g:link controller="experiment" class="menuButton" action="downloadAllZip" id="${experimentPInstance.id}"><img src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download whole experiment" ></g:link></g:if></td>
              <td>${fieldValue(bean: experimentPInstance, field: "topic")}</td>
              <td><g:formatDate type="datetime" style="medium" date="${experimentPInstance.createdOn}" /></td>

<!--            <td><g:link controller="experiment" class="menuButton" action="downloadPerform" id="${experimentPInstance.id}"><img src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download workbook" ></g:link></td>-->

            <td >
              <ul class="inline">
                <li>                
                  <img  style="cursor: pointer" onclick="uploadData('old','rawdata','${experimentPInstance.id}')"  src="${createLinkTo(dir:'images/ui', file:'Import.png')}" alt="upload existing gel inspector files" >
                </li>
                <g:if test="${experimentPInstance.resources.findAll{it.type=='rawdata'}.size()>0}">
                  <li>  
                  <g:link controller="experiment" class="menuButton" action="downloadRawData" id="${experimentPInstance.id}">
                    <img style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download all raw data" >
                  </g:link> 
                  
                  <g:link controller="resource"  action="list" params="[expId:experimentPInstance.id, type:'rawdata']" id="${experimentPInstance.id}">  (<font color="green"> ${experimentPInstance.resources.findAll{it.type=='rawdata'&& it.state=='active'}.size()}</font>|<font color="red">${experimentPInstance.resources.findAll{it.type=='rawdata' && it.state=='inactive'}.size()}</font>)</g:link>
                    <g:link controller="experiment" class="menuButton" action="generateExport" id="${experimentPInstance.id}" params="[layout:'gelinspector']"><img style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'export.png')}" alt="export into gelinspector" ></g:link>
                AutoGelInspector   
                
                </li>
                </g:if>



              </ul>
            </td>


            <td>
              <ul class="inline">
                <li>                
                  <img  style="cursor: pointer" onclick="uploadData('old','gelinspector','${experimentPInstance.id}')"  src="${createLinkTo(dir:'images/ui', file:'Import.png')}" alt="upload existing gel inspector files" >
                </li>
                <g:if test="${experimentPInstance.resources.findAll{it.type=='gelinspector'}.size()>0}">
                  <li>   <g:link controller="resource"  action="list" params="[expId:experimentPInstance.id, type:'gelinspector']" id="${experimentPInstance.id}">  (<font color="green"> ${experimentPInstance.resources.findAll{it.type=='gelinspector'&& it.state=='active'}.size()}</font>|<font color="red">${experimentPInstance.resources.findAll{it.type=='gelinspector' && it.state=='inactive'}.size()}</font>)</g:link>            
                  <g:link controller="experiment" class="menuButton" params="[type:'gelinspector']" action="downloadOrigExport" id="${experimentPInstance.id}"><img src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download gelInspector" ></g:link>
                  </li>
                </g:if>
              </ul>
            </td>
            
                   <td>
                <ul class="inline">             <li> 
                  <img  style="cursor: pointer" onclick="uploadData('old','other','${experimentPInstance.id}')"  src="${createLinkTo(dir:'images/ui', file:'Import.png')}" alt="upload other data" >
                </li>
                <g:if test="${experimentPInstance.resources.findAll{it.type=='other'}.size()>0}">  

                  <li> 
                  <g:link controller="experiment" class="menuButton" params="[type:'other']" action="downloadOrigExport" id="${experimentPInstance.id}">
                    <img style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download all other data" >
                  </g:link> 
                  <g:link controller="resource"  action="list" params="[expId:experimentPInstance.id, type:'other']" id="${experimentPInstance.id}">  (<font color="green"> ${experimentPInstance.resources.findAll{it.type=='other'&& it.state=='active'}.size()}</font>|<font color="red">${experimentPInstance.resources.findAll{it.type=='other' && it.state=='inactive'}.size()}</font>)</g:link>
                  </li>

                </g:if>
                <g:else>
                  <li> 
                    (<font size="2" color="green"> ${experimentPInstance.resources.findAll{it.type=='other'&& it.state=='active'}.size()}</font>|<font size="2" color="red">${experimentPInstance.resources.findAll{it.type=='other' && it.state=='inactive'}.size()}</font> )
                  </li>
                </g:else>
              </ul>
            </td>

            <td><g:formatDate  type="datetime" style="medium" date="${experimentPInstance?.lastUpdate()}" /><g:link controller="experiment" class="menuButton" action="downloadlog" id="${experimentPInstance.id}"><img src="${createLinkTo(dir:'images/ui', file:'log.png')}" alt="check log file" ></g:link>

            </td>
            <td>
            <g:if test="${experimentPInstance.share=='private'}"> Private 
              <img style="cursor: pointer" onclick="SharePExp('${experimentPInstance.id}')" src="${createLinkTo(dir:'images/ui', file:'shared.png')}" alt="make your experiment public" >

            </g:if> 
            <g:else> Public 
              <img style="cursor: pointer" onclick="StopSharePExp('${experimentPInstance.id}')" src="${createLinkTo(dir:'images/ui', file:'locked.png')}" alt="make your experiment private" >         
            </g:else>
            </td>
            <td> 
              <img  style="cursor: pointer" onclick="DeletePExp('${experimentPInstance.id}','${experimentPInstance.filename}')" src="${createLinkTo(dir:'images/ui', file:'trash.png')}" alt="delete the whole experiment" >
            </td>
            <td><img  style="cursor: pointer" onclick="Mail('${experimentPInstance.id}','updatePMe')" src="${createLinkTo(dir:'images/ui', file:'seek-logo.png')}" alt="upload to seek" >
            </td>
            </tr>
          </g:each>
          </tbody>
        </table>
      </div>

      <div id="ppager" class="pager" align="right"  style="height:50px; right:5% ">
        <g:form>
          <img src="${createLinkTo(dir:'images/ui', file:'first.png')}" class="first"/>
          <img src="${createLinkTo(dir:'images/ui', file:'prev.png')}" class="prev"/>
          <input style="width:80px" type="text" class="pagedisplay"/>
          <img src="${createLinkTo(dir:'images/ui', file:'next.png')}" class="next"/>
          <img src="${createLinkTo(dir:'images/ui', file:'last.png')}" class="last"/>
          <select style="width:50px"  class="pagesize">
            <option selected="selected" value="10">10</option>
            <option value="20">20</option>
            <option value="30">30</option>
            <option value="40">40</option>
          </select>
        </g:form>
      </div>
    </div>

  </body>
</html>
