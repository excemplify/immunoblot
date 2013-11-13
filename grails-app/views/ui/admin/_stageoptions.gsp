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

<div id="purposeOptions" title="Stages">
  <table style="width:100%"><tr><td colspan="2">
        The template will be saved under the name:  <input id="saveas" type="text" size="30" value="${oldname}"></td>
    </tr>
    <tr><td> <br></td></tr>
    <tr>
      <td style="font-family: serif; color: blue;"> Please select the <b>Stage</b> in which such template is used.

        <select id="purpose" style="width:60%" >
          <optgroup label="Considered Stages">
            <option selected="">setup</option>
      <!--      <option>loading</option>
            <option>rawdata</option>
            <option>gelInspector</option>-->
          </optgroup>
        </select> </td></tr>
    <tr><td> <br></td></tr>
    <tr><td>
        Please give some explanation/comment of your template  <input id="savecommentas" type="text" size="30" value=""></td></tr>
  </table>
</div>