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
<%@ page import="java.text.SimpleDateFormat" %>



<html>


  <body>


    <div class="body"  style="height:350px; width:100%" >
      <div  style="max-height: 350px; overflow: auto  ">
        <table id="experimentList" class="tablesorter">
          <thead>
            <tr>             
              <th>Experiment (zip)</th>
              <th>Type</th>
              <th>Setup Update<img style="cursor: pointer" onclick="warningMessage('We do not encourage such operation, but if you do so, please check those already uploaded raw data files for such experiment.');" src="${createLinkTo(dir:'images/ui', file:'attention.png')}" alt="update set up" ></th>
              <th>Created At</th>
              <th>Meta</th>
              <th>Rawdata Files<img style="cursor: pointer" onclick="warningMessage('.xls, .xlsx or special .txt files which is from your old machine');" src="${createLinkTo(dir:'images/ui', file:'attention.png')}" alt="raw data files comes out from your machines" ></th>
              <th>Export Options<img style="cursor: pointer" onclick="warningMessage('rdf and snippet export will only include gelinspector information if your export the gelinspector first.');" src="${createLinkTo(dir:'images/ui', file:'attention.png')}" ></th>
              <th>Other Files<img style="cursor: pointer" onclick="warningMessage('Here you can upload other non excel files. e.g. image file, pdf file');" src="${createLinkTo(dir:'images/ui', file:'attention.png')}" alt="other related files" ></th>
              <th>State(sharing)</th>
              <th>Delete</th>
              <th>Send To SEEK??</th>
            </tr>
          </thead>
          <tbody>

          <g:each  in="${experimentInstanceList}" status="i" var="experimentInstance">

            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
              <td style=" white-space:pre-line; text-align: justify"><b>${i+1}. </b>${fieldValue(bean: experimentInstance, field: "filename")}<g:if test="${experimentInstance.resources.findAll{it.type=='rawdata'&& it.state=='active'}.size()>0}"><g:link controller="experiment" class="menuButton" action="downloadAllZip" id="${experimentInstance.id}"><img src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download whole experiment" ></g:link></g:if></td>
              <td>${fieldValue(bean: experimentInstance, field: "topic")}</td>
              <td><img onclick="showUpdateDialog('${experimentInstance.id}','${experimentInstance.filename}')" style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'update.png')}" alt="update set up" ></td>
              <td><g:formatDate type="datetime" style="medium" date="${experimentInstance.createdOn}" /></td>

            <td><g:link controller="experiment" class="menuButton" action="download" id="${experimentInstance.id}"><img src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download loading data" ></g:link></td>


            <td >       <ul class="inline">             <li> 
                  <img  style="cursor: pointer" onclick="uploadData('new','rawdata','${experimentInstance.id}')"  src="${createLinkTo(dir:'images/ui', file:'Import.png')}" alt="upload raw data" >
                </li>
                <g:if test="${experimentInstance.resources.findAll{it.type=='rawdata'}.size()>0}">  

                  <li> 
                  <g:link controller="experiment" class="menuButton" action="downloadRawData" id="${experimentInstance.id}">
                    <img style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download all raw data" >
                  </g:link> 
                  <g:link controller="resource"  action="list" params="[expId:experimentInstance.id, type:'rawdata']" id="${experimentInstance.id}">  (<font color="green"> ${experimentInstance.resources.findAll{it.type=='rawdata'&& it.state=='active'}.size()}</font>|<font color="red">${experimentInstance.resources.findAll{it.type=='rawdata' && it.state=='inactive'}.size()}</font>)</g:link>
                  </li>

                </g:if>
                <g:else>
                  <li> 
                    (<font size="2" color="green"> ${experimentInstance.resources.findAll{it.type=='rawdata'&& it.state=='active'}.size()}</font>|<font size="2" color="red">${experimentInstance.resources.findAll{it.type=='rawdata' && it.state=='inactive'}.size()}</font> )
                  </li>
                </g:else>
              </ul>
            </td>
            <td> 
            <g:if test="${experimentInstance.resources.findAll{it.type=='rawdata'&& it.state=='active'}.size()>0}">
              <ul class="inline">
                <li> <g:link controller="experiment" class="menuButton" action="exportInto" id="${experimentInstance.id}" params="[layout:'gelinspector']"><img style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'export.png')}" alt="export into gelinspector" ></g:link>
                GelInspector
                </li>
                <li>
                <g:link controller="experiment" class="menuButton" action="exportRdf" id="${experimentInstance.id}" params="[layout:'rdf']"><img style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'rdf.png')}" alt="export into rdf" ></g:link>
                RDF
                </li>
                <li> <g:link controller="experiment" class="menuButton" action="downloadSnippet" id="${experimentInstance.id}" params="[layout:'html']"><img style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'labbook.png')}" alt="export lab book snippet" ></g:link>
                Snippet</li>
              </ul>
            </g:if>
            <g:else>
              no raw data yet

            </g:else>

            </td>
<!--            <td><g:formatDate  type="datetime" style="medium" date="${experimentInstance?.lastUpdate()}" /><g:link controller="experiment" class="menuButton" action="downloadlog" id="${experimentInstance.id}"><img src="${createLinkTo(dir:'images/ui', file:'log.png')}" alt="check log file" ></g:link>
<g:if test="${experimentInstance.createdOn.compareTo(new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).parse("2012-09-07 00:00:00")) > 0}">
<g:link controller="experiment" class="menuButton" action="visualizelog" id="${experimentInstance.id}">visualize</g:link>
</g:if>
            </td>-->
            <td>
                <ul class="inline">             <li> 
                  <img  style="cursor: pointer" onclick="uploadData('new','other','${experimentInstance.id}')"  src="${createLinkTo(dir:'images/ui', file:'Import.png')}" alt="upload other data" >
                </li>
                <g:if test="${experimentInstance.resources.findAll{it.type=='other'}.size()>0}">  

                  <li> 
                  <g:link controller="experiment" class="menuButton" params="[type:'other']" action="downloadOrigExport" id="${experimentInstance.id}">
                    <img style="cursor: pointer" src="${createLinkTo(dir:'images/ui', file:'download.png')}" alt="download all other data" >
                  </g:link> 
                  <g:link controller="resource"  action="list" params="[expId:experimentInstance.id, type:'other']" id="${experimentInstance.id}">  (<font color="green"> ${experimentInstance.resources.findAll{it.type=='other'&& it.state=='active'}.size()}</font>|<font color="red">${experimentInstance.resources.findAll{it.type=='other' && it.state=='inactive'}.size()}</font>)</g:link>
                  </li>

                </g:if>
                <g:else>
                  <li> 
                    (<font size="2" color="green"> ${experimentInstance.resources.findAll{it.type=='other'&& it.state=='active'}.size()}</font>|<font size="2" color="red">${experimentInstance.resources.findAll{it.type=='other' && it.state=='inactive'}.size()}</font> )
                  </li>
                </g:else>
              </ul>
            </td>
            <td>
            <g:if test="${experimentInstance.share=='private'}"> Private 
              <img style="cursor: pointer" onclick="ShareExp('${experimentInstance.id}')" src="${createLinkTo(dir:'images/ui', file:'shared.png')}" alt="make your experiment public" >

            </g:if> 
            <g:else> Public 
              <img style="cursor: pointer" onclick="StopShareExp('${experimentInstance.id}')" src="${createLinkTo(dir:'images/ui', file:'locked.png')}" alt="make your experiment private" >         
            </g:else>
            </td>
            <td> 
              <img  style="cursor: pointer" onclick="DeleteExp('${experimentInstance.id}','${experimentInstance.filename}')" src="${createLinkTo(dir:'images/ui', file:'trash.png')}" alt="delete the whole experiment" >
            </td>
            <td><img  style="cursor: pointer" onclick="Mail('${experimentInstance.id}','updateMe')" src="${createLinkTo(dir:'images/ui', file:'seek-logo.png')}" alt="upload to seek" >
            </td>
            </tr>
          </g:each>
          </tbody>
        </table>
      </div>
      <div id="pager" class="pager" align="right"  style="height:50px; right:5%">
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
