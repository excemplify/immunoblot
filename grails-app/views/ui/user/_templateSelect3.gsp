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

<select name="template3" style=" width:350px;" id="template3">
          <g:each in="${templateInstanceList}" status="i" var="templateInstance">
             <option value="${templateInstance.id}">${i+1}. ${fieldValue(bean: templateInstance, field: "templateName")}</option>
        </g:each>
</select>