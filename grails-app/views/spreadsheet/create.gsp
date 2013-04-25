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
<%@ page import="org.hits.parser.Spreadsheet" %>

<html>
    <head>
        
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'spreadsheet.label', default: 'Spreadsheet')}" />
        <title><g:message code="default.create.label" args="[entityName]" /></title>
        
    </head>
    <body>
   
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></span>
            <span class="menuButton"><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></span>
        </div>
        <div class="body">
            <h1><g:message code="default.create.label" args="[entityName]" /></h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${spreadsheetInstance}">
            <div class="errors">
                <g:renderErrors bean="${spreadsheetInstance}" as="list" />
            </div>
            </g:hasErrors>
           
           <g:uploadForm action="save" method="post">
             <div>
               
                <table>
                  <tr class="prop">
               <td valign="top" class="name">
                          <label for="payload">File:</label>
                        </td>
                        <td valign="top">
                        <input type="file" id="payload" name="payload"/>
                        </td>
                        </tr>
              <tr class="prop">
                <td>Parser</td><td><g:select name="parserType" from="${parserDefList}"/></td>
                <td>Spreadsheet Name</td><td><g:textField name="expName" /></td>
                </tr>
                  </table>
              </div>
        
                <div class="buttons">
                    <span class="button"><g:submitButton name="create" class="save" value="${message(code: 'default.button.create.label', default: 'Save and move to next stage')}" /></span>
                </div>
            </g:uploadForm>
        </div>

    </body>

</html>
