<#ftl strip_whitespace=true>

<#--
  - File: permissions.ftl
  - 
  - Description: A HTML page that displays the permissions set on a
  - resource
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/permissions.ftl" as permissions />
<#import "/lib/autocomplete.ftl" as autocomplete />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Permissions on resource</title>
    
    <@autocomplete.addAutoCompleteScripts srcBase="${webResources?html}"/>
    <script type='text/javascript' src='${webResources?html}/autocomplete/autocomplete.permissions.js'></script>
    
    <script type="text/javascript">
      $(document).ready(function() {
        interceptEnterKeyAndReroute("input#groupNames", "input.addGroupButton");
        var permissionsAutocompletePrams = {minChars:4, selectFirst:false, width:300, max:30, delay:800};
        permissionsAutocomplete('userNames', 'userNames', permissionsAutocompletePrams);
        splitAutocompleteSuggestion('userNames');
        permissionsAutocomplete('groupNames', 'groupNames', permissionsAutocompletePrams);
      });
    </script>
    
  </head>
  <body>

    <#assign resource = resourceContext.currentResource />
    <#assign defaultHeader = vrtx.getMsg("permissions.header", "Permissions on this resource") />

    <div class="resourceInfo permissions">
      <h2>
        <@vrtx.msg
           code="permissions.header.${resource.resourceType}"
           default="${defaultHeader}"/>
      </h2>

      <#assign defaultNotInherited = vrtx.getMsg("permissions.notInherited", "Custom permissions") />



      <#if aclInfo.aclEditURLs?exists && aclInfo.aclEditURLs.inheritance?exists>
      <form action="${aclInfo.aclEditURLs.inheritance?html}" method="post"
            id="permissions.toggleInheritance" class="vrtx-admin-button">
        <#if resourceContext.currentResource.inheritedAcl>
          <@vrtx.msg code="permissions.isInherited" default="Inherited permissions" />
          <#if aclInfo.aclEditURLs.inheritance?exists>
            <input type="submit" id="permissions.toggleInheritance.submit"
                   name="confirmation" value="<@vrtx.msg code="permissions.setCustom" default="edit" />" />
          </#if>
        <#else>
          <#assign warning =
                   vrtx.getMsg("permissions.removeAcl.warning", 
                   "Are you sure you want to set inherited permissions? This cannot be undone.",
                   [resource.name]) />
          <@vrtx.msg code="permissions.notInherited.${resource.resourceType}" default="${defaultNotInherited}" />
          <#if aclInfo.aclEditURLs.inheritance?exists>
            <input type="submit"
                   onclick="return confirm('${warning?html?js_string}');" 
                   id="permissions.toggleInheritance.submit"
                   name="confirmation" value="<@vrtx.msg code="permissions.setInherited" default="edit" />" />
          </#if>
        </#if>
      </form>
      </#if>

      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.read" default="Read" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="read" privilegeHeading=privilegeHeading />

      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.write" default="Write" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="write" privilegeHeading=privilegeHeading />

      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.all" default="Admin - all privileges" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="all" privilegeHeading=privilegeHeading />
     
      <#assign groupHeading><@vrtx.msg code="permissions.advanced" default="Advanced permissions" /></#assign>

      <#assign commentHeading><@vrtx.msg code="permissions.privilege.add-comment" default="Add comments" /></#assign>
      <#assign bindHeading><@vrtx.msg code="permissions.privilege.bind" default="Create resources only" /></#assign>
      <#assign readProHeading><@vrtx.msg code="permissions.privilege.read-processed" default="Read processed only" /></#assign>
      <#if resource.collection>
        <#assign privilegeList = [{"name":"add-comment", "heading":commentHeading }, {"name":"bind", "heading": bindHeading}, {"name":"read-processed", "heading":readProHeading }] />
      <#else>
        <#assign privilegeList = [{"name":"add-comment", "heading":commentHeading }, {"name":"read-processed", "heading":readProHeading }] />
      </#if>        
      <@permissions.editOrDisplayPrivileges privilegeList=privilegeList heading=groupHeading />

    </div>
  </body>
</html>

