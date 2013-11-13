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
<table class="templateSelect">
  <tr>
     <td colspan="2" ><label><b>Using Templates (Stage Specific):</b></label></td>
  </tr>
<tr >
  <td style=" width:50px"><label for="template">Setup</label> 
  </td>
  <td style=" width:500px">
    <select name="template2" id="template2" style=" width:500px">
      <g:each in="${templateInstanceList}" status="i" var="templateInstance">
        <option value="${templateInstance.id}">${i+1}. ${fieldValue(bean: templateInstance, field: "templateName")}</option>
      </g:each>
    </select>
  </td>
</tr>
<!--<tr >
  <td style=" width:60px"><label for="templateloading">Loading</label></td>
  <td>
    <select name="templateloading2"  id="templateloading2">
      <option value="default">1.default loading template (inner)</option> 
      <g:each in="${templateLoadingInstanceList}" status="i" var="templateLoadingInstance">
        <option value="${templateLoadingInstance.id}">${i+2}. ${fieldValue(bean: templateLoadingInstance, field: "templateName")}</option>
      </g:each>
    </select>
  </td>
</tr>
<tr>
  <td style=" width:60px"><label for="templaterawdata">Raw Data </label></td>
  <td>
    <select name="templaterawdata2" id="templaterawdata2">
      <option value="default">1.default raw data template (inner)</option> 
      <g:each in="${templateRawDataInstanceList}" status="i" var="templateRawDataInstance">
        <option value="${templateRawDataInstance.id}">${i+2}. ${fieldValue(bean: templateRawDataInstance, field: "templateName")}</option>
      </g:each>
    </select>
  </td>
</tr>
<tr >
  <td style=" width:60px"><label for="templategelinspector">GelInspector</label>
    </td>
  <td>
    <select name="templategelinspector2"  id="templategelinspector2">
      <option value="default">1.default gelInspector (inner)</option> 
      <g:if test="${templateGelInstanceList?.size()>0}">
      <g:each in="${templateGelInstanceList}" status="i" var="templateGelInstance">
        <option value="${templateGelInstance.id}">${i+2}. ${fieldValue(bean: templateGelInstance, field: "templateName")}</option>
      </g:each>
      </g:if>
    </select>
  </td>
</tr>-->

</table>

