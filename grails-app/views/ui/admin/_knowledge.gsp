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


<div >
  <div class="tableheader">Concepts Mapping</div>
  <g:if test="${templateKnowledgeList}">
    <div class="tablecontent">
      <table class="styletable">
        <thead>
          <tr>
            <td><b>Concept</b></td>
            <td><b>Corresponds To</b></td>
            <td><b>Sheet Index</b></td>
            <td><b>Location Range</b></td>
          
        </thead>
        <tbody>
        <g:each in="${templateKnowledgeList}" status="i" var="knowledgeInstance">
          <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
  <!--          <td>${knowledgeInstance.id}</td>-->
            <td>${knowledgeInstance.knowledgeName}</td>
            <td>${knowledgeInstance.columnName}</td>
            <td>${knowledgeInstance.sheetIndex}</td>
            <td>${knowledgeInstance.markCellRange}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
  </g:if>
</div>
