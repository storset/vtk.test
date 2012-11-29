<#ftl strip_whitespace=true />
<#import "/lib/menu/list-menu.ftl" as listMenu />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/ping.ftl" as ping />

<#assign resource = resourceContext.currentResource />

<script type="text/javascript"><!--
  if(vrtxAdmin.isIE7 || vrtxAdmin.isIETridentInComp) {
    if(vrtxAdmin.isIETridentInComp) {
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.comp")}';   
    } else if(vrtxAdmin.isIE6) {   
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.msie7")}';    
    } else if(vrtxAdmin.isIE7) {
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.msie6")}';
    }
  }
  var cancelI18n = '${vrtx.getMsg("editor.cancel")}';
// -->
</script>

<#if pingURL?? && !resourceContext.currentServiceName?lower_case?contains("preview")>
  <@ping.ping url=pingURL['url'] interval=300/> 
</#if>

<#if resource?exists && resourceMenuLeft?exists && resourceMenuRight?exists>
  <@gen resource resourceMenuLeft resourceMenuRight />
<#elseif resource?exists && resourceMenuLeft?exists>
  <@gen resource resourceMenuLeft />
<#else>  
  <@gen resource /> 
</#if>

<#macro gen resource resourceMenuLeft="" resourceMenuRight="">
  <div id="title-container">
    <#local compactClass = "" />
    ${resourceMenuRightServicesLinkable}
    <#if resourceMenuLeftServicesLinkable == 0
       && (resourceMenuRightServicesLinkable == 0
       || (resourceMenuRightServicesLinkable == 1 && unlockPermission.permissionsQueryResult = 'false')
       || (resourceMenuRightServicesLinkable == 1 && writePermission.permissionsQueryResult = 'false')
       || (resourceMenuRightServicesLinkable == 1 && !unpublishLink.url??)
       || (resourceMenuRightServicesLinkable == 1 && !publishLink.url??)
       || (resourceMenuRightServicesLinkable == 2 && writePermission.permissionsQueryResult = 'false' && unlockPermission.permissionsQueryResult = 'false')
       )>
      <#local compactClass = " smaller-seperator" />    
    </#if>
    <div id="resource-title" class="<@vrtx.resourceToIconResolver resource /> ${resource.collection?string}${compactClass}">
      <h1>
        <#if resource.URI == '/'>
          ${repositoryID?html}
        <#else>
          <@vrtx.breakSpecificChar nchars=40 splitClass="title">${resource.name?html}</@vrtx.breakSpecificChar>
        </#if>
      </h1>
      <#if browseURL?exists && editField?exists><#-- TODO: fix this hack for browse -->
        <ul class="list-menu" id="resourceMenuLeft">
          <li class="createLinkToResourceService first last">
            <a href="javascript:updateParent('${editField}', '${browseURL}')"><@vrtx.msg code="resourceMenuLeft.createLinkToResourceService" default="Create link" /></a>
          </li>
        </ul>
      </#if>

      <#if resourceMenuRight != "">
        <@listMenu.listMenu menu=resourceMenuRight displayForms=true prepend="" append=""/>
      </#if>
      <#if resourceMenuLeft != "">
        <@listMenu.listMenu menu=resourceMenuLeft displayForms=true prepend="" append=""/>
      </#if>
    </div>
  </div>
</#macro>