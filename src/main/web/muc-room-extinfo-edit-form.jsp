<%@ page contentType="text/html; charset=UTF-8" %>
<!--
- Copyright (C) 2019 Ignite Realtime Foundation. All rights reserved.
-
- Licensed under the Apache License, Version 2.0 (the "License");
- you may not use this file except in compliance with the License.
- You may obtain a copy of the License at
-
- http://www.apache.org/licenses/LICENSE-2.0
-
- Unless required by applicable law or agreed to in writing, software
- distributed under the License is distributed on an "AS IS" BASIS,
- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
- See the License for the specific language governing permissions and
- limitations under the License.
-->
<%@ page import="org.igniterealtime.openfire.plugin.mucextinfo.DAO" %>
<%@ page import="org.igniterealtime.openfire.plugin.mucextinfo.ExtDataForm" %>
<%@ page import="org.jivesoftware.openfire.muc.MUCRoom" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="org.xmpp.packet.JID" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.igniterealtime.openfire.plugin.mucextinfo.MucExtInfoPlugin" %>
<%@ page import="org.xmpp.forms.DataForm" %>
<%@ page import="java.util.*" %>
<%@ page import="org.xmpp.forms.FormField" %>
<%@ page import="org.igniterealtime.openfire.plugin.mucextinfo.Field" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.function.Function" %>
<%@ page errorPage="error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="admin" prefix="admin" %>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>
<%
    String success = request.getParameter("success");
    JID roomJID = new JID(ParamUtils.getParameter(request, "roomJID"));
    boolean addForm = request.getParameter("addForm") != null;
    boolean deleteForm = request.getParameter("deleteForm") != null;
    boolean addField = request.getParameter("addField") != null;
    boolean deleteField = request.getParameter("deleteField") != null;
    String formTypeName = request.getParameter("formTypeName");
    String varName = request.getParameter("varName");
    String label = request.getParameter("label");
    String value = request.getParameter("value");

    // Load the room object
    MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomJID.getNode());
    if ( room == null ) {
        response.sendError(404);
        return;
    }

    Map<String, String> errors = new HashMap<>();

    final Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");

    // Validation.
    final List<ExtDataForm> extensionsOnly = DAO.retrieveExtensionElementsForRoom(roomJID);
    if (addForm || deleteForm || addField || deleteField)
    {
        if ( csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals( csrfParam ) )
        {
            errors.put("csrf", "CSRF Error. Please reload the page and try again.");
            addForm = false;
            deleteForm = false;
            addField = false;
            deleteField = false;
        }

        if ( formTypeName == null || formTypeName.trim().isEmpty() ) {
            errors.put("formTypeName", "Missing Form Name");
        }

        if ( addForm ) {
            List<ExtDataForm> existing = extensionsOnly;
            if ( existing != null && existing.stream().anyMatch(dataForm -> dataForm.getFormTypeName().equalsIgnoreCase(formTypeName)) ) {
                errors.put( "already exists", "A form with this name already exists!");
            }
        }

        if ( deleteForm ) {
            List<ExtDataForm> existing = extensionsOnly;
            if ( existing == null || existing.stream().noneMatch(dataForm -> dataForm.getFormTypeName().equalsIgnoreCase(formTypeName)) ) {
                errors.put( "does not exist", "A form with this name does not exist!");
            }
        }

        if ( addField ) {
            if ( varName == null || varName.trim().isEmpty() ) {
                errors.put("varName", "Missing variable name.");
            }
        }

        if ( deleteField ) {
            if ( varName == null || varName.trim().isEmpty() ) {
                errors.put("varName", "Missing variable name.");
            } else {
                List<ExtDataForm> existing = extensionsOnly;
                if ( existing == null || existing.stream().noneMatch(dataForm -> dataForm.getFields().stream().anyMatch(field -> field.getVarName().equals(varName)) ) )
                {
                    errors.put("does not exist", "A field with this name does not exist in the form!");
                }
            }
        }
    }

    // Apply changes
    if ( errors.isEmpty() ) {
        if ( addForm ) {
            DAO.addForm( roomJID, formTypeName );

            webManager.logEvent( "MUC External Info, new form added.", "form name: " + formTypeName );
            response.sendRedirect( "muc-room-extinfo-edit-form.jsp?roomJID="+URLEncoder.encode(roomJID.toBareJID(), "UTF-8") + "&success=true" );
            return;
        } else if ( deleteForm ) {
            DAO.removeForm( roomJID, formTypeName );

            webManager.logEvent( "MUC External Info, form deleted.", "form name: " + formTypeName );
            response.sendRedirect( "muc-room-extinfo-edit-form.jsp?roomJID="+URLEncoder.encode(roomJID.toBareJID(), "UTF-8") + "&success=true" );
            return;
        } else if ( addField ) {
            DAO.addField( roomJID, formTypeName, varName, label, value );

            webManager.logEvent( "MUC External Info, new field added.", "form name: " + formTypeName + ", field varName: " + varName );
            response.sendRedirect( "muc-room-extinfo-edit-form.jsp?roomJID="+URLEncoder.encode(roomJID.toBareJID(), "UTF-8") + "&success=true" );
            return;
        } else if ( deleteField ) {
            DAO.removeField( roomJID, formTypeName, varName );

            webManager.logEvent( "MUC External Info, field deleted.", "form name: " + formTypeName + ", field varName: " + varName );
            response.sendRedirect( "muc-room-extinfo-edit-form.jsp?roomJID="+URLEncoder.encode(roomJID.toBareJID(), "UTF-8") + "&success=true" );
            return;
        }
    } else {
        // Repopulate input fields with potentially erroneous data, to be corrected by the end user.
        if ( addField )
        {
            pageContext.setAttribute("varName", varName);
            pageContext.setAttribute("label", label);
            pageContext.setAttribute("value", value);
        }

        if ( addForm ) {
            pageContext.setAttribute("formTypeName", formTypeName);
        }
    }

    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute( "csrf", csrfParam) ;
    pageContext.setAttribute( "errors", errors );
    pageContext.setAttribute( "roomJID", roomJID );
    pageContext.setAttribute( "success", success != null && errors.isEmpty() );

    // All of the dataforms (including the extensions provided by this implementation).
    final Set<DataForm> originalForms = MucExtInfoPlugin.getProvider(webManager.getMultiUserChatManager().getMultiUserChatService(roomJID)).getExtendedInfos(roomJID.getNode(), null, roomJID);
    final List<ExtDataForm> extDataForms = new ArrayList<>();
    for ( final DataForm originalForm : originalForms ) {
        final ExtDataForm extDataForm = new ExtDataForm( originalForm.getField("FORM_TYPE").getFirstValue() );
        for ( final FormField formField : originalForm.getFields() ) {
            // Exclude the 'form type' field.
            if ( !formField.getVariable().equals("FORM_TYPE") )
            {
                extDataForm.getFields().add(new Field(formField.getVariable(), formField.getLabel(), formField.getValues().toArray(new String[0])));
            }
        }
        extDataForms.add(extDataForm);
    }

    // This collection is used for presentation purposes. Apply a consistent ordering, otherwise page reloads yield confusing results.
    extDataForms.sort(Comparator.comparing(ExtDataForm::getFormTypeName));
    pageContext.setAttribute( "extendedForms", extDataForms );

    // A collection of just the extensions managed by this plugin, mapped by their form type name.
    final Map<String, ExtDataForm> extensionsMappedByName;
    if ( extensionsOnly == null ) {
        extensionsMappedByName = Collections.emptyMap();
    } else {
        extensionsMappedByName = extensionsOnly.stream().collect(Collectors.toMap(ExtDataForm::getFormTypeName, Function.identity()));
    }
    pageContext.setAttribute( "extensionsOnly", extensionsMappedByName );
%>
<html>
<head>
    <title><fmt:message key="mucextinfo.page.title"/></title>
    <meta name="subPageID" content="muc-room-extinfo-edit-form"/>
    <meta name="extraParams" content="<%= "roomJID="+URLEncoder.encode(roomJID.toBareJID(), "UTF-8") %>"/>
</head>
<body>

<c:forEach var="err" items="${errors}">
    <admin:infobox type="error">
        <c:choose>
            <c:when test="${err.key eq 'csrf'}"><fmt:message key="global.csrf.failed" /></c:when>
            <c:otherwise>
                <c:if test="${not empty err.value}">
                    <fmt:message key="admin.error"/>: <c:out value="${err.value}"/>
                </c:if>
                (<c:out value="${err.key}"/>)
            </c:otherwise>
        </c:choose>
    </admin:infobox>
</c:forEach>

<c:if test="${success}">
    <admin:infoBox type="success">
        <fmt:message key="settings.saved.successfully" />
    </admin:infoBox>
</c:if>

<p>
    <fmt:message key="mucextinfo.page.description"/>
</p>

<br>

<c:forEach var="extendedForm" items="${extendedForms}">

    <c:set var="formContainsUnmodifiableFields" value="false"/>

    <form action="muc-room-extinfo-edit-form.jsp?edit" method="post">
        <input type="hidden" name="csrf" value="${csrf}">
        <input type="hidden" name="roomJID" value="${fn:escapeXml(roomJID.toBareJID())}">
        <input type="hidden" name="formTypeName" value="${fn:escapeXml(extendedForm.formTypeName)}">

        <fieldset>
            <legend>
                <fmt:message key="mucextinfo.page.form.legend">
                    <fmt:param value="${extendedForm.formTypeName}"/>
                </fmt:message>
            </legend>

            <div style="width: unset">
                <p><fmt:message key="mucextinfo.page.form.fields.description" /></p>

                <div class="jive-table">
                    <table cellpadding="0" cellspacing="0" border="0" width="100%">
                        <thead>
                            <tr>
                                <th>&nbsp;</th>
                                <th nowrap><fmt:message key="mucextinfo.page.form.fields.varname" /></th>
                                <th nowrap><fmt:message key="mucextinfo.page.form.fields.label" /></th>
                                <th nowrap><fmt:message key="mucextinfo.page.form.fields.values" /></th>
                                <th nowrap><fmt:message key="mucextinfo.page.form.fields.delete" /></th>
                            </tr>
                        </thead>

                        <tbody>
                        <c:choose>
                            <c:when test="${empty extendedForm.fields}">
                        <tr>
                            <td align="center" colspan="6">
                                <fmt:message key="mucextinfo.page.form.fields.no-fields" />
                            </td>
                        </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="field" items="${extendedForm.fields}" varStatus="status">
                                    <tr class="jive-${status.index%2 == 0 ? 'even' : 'odd'}">
                                        <td width="1%">&nbsp;</td>
                                        <td><c:out value="${field.varName}"/></td>
                                        <td><c:out value="${field.label}"/></td>
                                        <td>
                                            <c:forEach var="val" items="${field.values}">
                                                <c:out value="${val}"/> <br/>
                                            </c:forEach>
                                        </td>
                                        <td width="1%">
                                            <c:choose>
                                                <c:when test="${not empty extensionsOnly[extendedForm.formTypeName] and extensionsOnly[extendedForm.formTypeName].fields.contains(field) }">
                                                    <a href="muc-room-extinfo-edit-form.jsp?deleteField=true&csrf=${csrf}&roomJID=${admin:urlEncode(roomJID)}&formTypeName=${admin:urlEncode(extendedForm.formTypeName)}&varName=${admin:urlEncode(field.varName)}"
                                                       title="<fmt:message key="mucextinfo.page.click-to-delete" />"
                                                       onclick="return confirm('<fmt:message key="mucextinfo.page.field-delete-confirm"/>');">
                                                        <img src="../../images/delete-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="mucextinfo.page.click-to-delete" />">
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:set var="formContainsUnmodifiableFields" value="true"/>
                                                    &nbsp;
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                        </tbody>
                    </table>

                </div>

                <br/>
                <p><b><fmt:message key="mucextinfo.page.add.header"/></b></p>
                <table border="0">
                    <tr>
                        <td><label for="varName"><fmt:message key="mucextinfo.page.add.field.varname" /></label></td>
                        <td><input name="varName" id="varName" type="text" size="30" value="${varName != null ? admin:escapeHTMLTags(varName) : ''}"/>
                            <c:if test="${not empty param.addField && not empty errors['varName']}">
                                <span style="color:red"><c:out value="${errors['varName']}"/></span>
                            </c:if>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="label"><fmt:message key="mucextinfo.page.add.field.label" /></label></td>
                        <td><input name="label" id="label" type="text" size="30" value="${label != null ? admin:escapeHTMLTags(label) : ''}"/>
                            <c:if test="${not empty param.addField && not empty errors['label']}">
                                <span style="color:red"><c:out value="${errors['label']}"/></span>
                            </c:if>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="value"><fmt:message key="mucextinfo.page.add.field.value" /></label></td>
                        <td><input name="value" id="value" type="text" size="30" value="${value != null ? admin:escapeHTMLTags(value) : ''}"/>
                            <c:if test="${not empty param.addField && not empty errors['value']}">
                                <span style="color:red"><c:out value="${errors['value']}"/></span>
                            </c:if>
                        </td>
                    </tr>
                </table>

                <br/>
                <table border="0" width="100%">
                    <tr>
                        <td><input type="submit" name="addField" value="<fmt:message key="mucextinfo.page.add-field" />" /></td>
                        <c:choose>
                            <c:when test="${formContainsUnmodifiableFields}">
                                <td align="right"><input type="submit" name="deleteForm" value="<fmt:message key="mucextinfo.page.delete-all-extension-fields" />" onclick="return confirm('<fmt:message key="mucextinfo.page.extension-fields-delete-confirm"/>');" /></td>
                            </c:when>
                            <c:otherwise>
                                <td align="right"><input type="submit" name="deleteForm" value="<fmt:message key="mucextinfo.page.delete-form" />" onclick="return confirm('<fmt:message key="mucextinfo.page.form-delete-confirm"/>');" /></td>
                            </c:otherwise>
                        </c:choose>
                    </tr>
                </table>

            </div>

        </fieldset>

    </form>

    <br/>

</c:forEach>

<form action="muc-room-extinfo-edit-form.jsp?addForm" method="post">
    <input type="hidden" name="csrf" value="${csrf}">
    <input type="hidden" name="roomJID" value="${fn:escapeXml(roomJID.toBareJID())}">

    <p>
        <label for="formTypeName"><fmt:message key="mucextinfo.page.new.form-name" /></label>
        <input type="text" size="60" id="formTypeName" name="formTypeName" value="${formTypeName != null ? admin:escapeHTMLTags(formTypeName) : ''}"/>
        <input type="submit" name="add" value="<fmt:message key="mucextinfo.page.create-form" />" />
        <c:if test="${not empty param.addForm and not empty errors['formTypeName']}">
            <span style="color:red"><c:out value="${errors['formTypeName']}"/></span>
        </c:if>
    </p>
</form>

</body>
</html>
