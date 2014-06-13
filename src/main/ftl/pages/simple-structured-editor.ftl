<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/editor/common.ftl" as editor />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#assign htmlTitle = vrtx.getMsg("message-listing.new-message") />
  <#if properties?exists>
    <#assign htmlTitle = vrtx.getMsg("message-listing.edit-message") />
  </#if>
  <title>${htmlTitle}</title>
  <#include "/system/css.ftl" />
  <style type="text/css">
    html {
      background: #999999;
    }
  </style>

  <#global baseFolder = "/" />
  <#if resourceContext.parentURI?exists>
    <#if isCollection?exists && isCollection>
      <#global baseFolder = resourceContext.currentURI?html />
    <#else>
      <#global baseFolder = resourceContext.parentURI?html />
    </#if>
  </#if>
  <#include "/system/javascript.ftl" />
  <script type="text/javascript"><!-- 
    vrtxAdmin.multipleFormGroupingMessages = {
      add: "${vrtx.getMsg('editor.add')}",
      remove: "${vrtx.getMsg('editor.remove')}",
      moveUp: "${vrtx.getMsg('editor.move-up')}",
      moveDown: "${vrtx.getMsg('editor.move-down')}",
      browse: "${vrtx.getMsg('editor.browseImages')}"
    };
	vrtxAdmin.multipleFormGroupingPaths = {
	  <#if fckeditorBase??>
	  baseBrowserURL: "${fckeditorBase.url?html}",
	  baseFolderURL: "${baseFolder}",
	  baseDocURL: "${fckeditorBase.documentURL?html}",
	  basePath: "${fckBrowse.url.pathRepresentation}"
	  </#if>
	};
	if(vrtxAdmin.hasFreeze) { // Make immutables
	  Object.freeze(vrtxAdmin.multipleFormGroupingMessages);
	  Object.freeze(vrtxAdmin.multipleFormGroupingPaths);
	}
  
    // CKEditor CSS
    var cssFileList = [<#if fckEditorAreaCSSURL?exists>
                         <#list fckEditorAreaCSSURL as cssURL>
                           "${cssURL?html}" <#if cssURL_has_next>,</#if>
                         </#list>
                       </#if>]; 
  // -->
  </script>
  <@editor.addCkScripts />
  <@editor.createEditor 'message' false false />
  <script type="text/javascript"><!--
    if(typeof vrtxAdmin !== "undefined") {
      vrtxAdmin.runReadyLoad = false;
    }
    $(function() {
      var centerFromTop = (($(window).outerHeight() / 2) - $("#app-content").outerHeight());
      centerFromTop = !isNaN(centerFromTop) ? centerFromTop : 20;
      $("#app-content").css("marginTop", centerFromTop + "px"); 
    });  
  // -->
  </script>
</head>
<body id="vrtx-simple-editor" class="forms-new">
<div id="app-content">
  <#if isNew??>
    <h3>${vrtx.getMsg("message-listing.new-message")}<a href="javascript:void(0)" id="vrtx-close-simple-structured-editor" class="vrtx-close-dialog-editor"></a></h3>
  <#else>
    <h3>${vrtx.getMsg("message-listing.edit-message")}<a href="javascript:void(0)" id="vrtx-close-simple-structured-editor" class="vrtx-close-dialog-editor"></a></h3>
  </#if>
  <#if url?exists>
    <form action="" method="post">
      <@vrtx.csrfPreventionToken url />
      <div class="properties">
        <div id="vrtx-resource.userTitle" class="userTitle property-item">
          <div class="property-label">
            ${vrtx.getMsg("property.title")}
          </div>
          <input class="vrtx-textfield" type="text" name="title" id="title"<#if properties?exists && properties.title?exists> value="${properties.title?html}"</#if> />
        </div>
        <div id="vrtx-message" class="property-item">
          <div class="property-label">
            ${vrtx.getMsg("resourcetype.name.structured-message")}
          </div>
          <textarea id="message" name="message"><#if properties?exists && properties.message?exists>${properties.message?html}</#if></textarea>
        </div>
      </div>
      <input class="vrtx-focus-button" type="submit" id="save" name="save" value="${vrtx.getMsg("editor.save")}" />
    </form>
    <form action="" method="post" id="vrtx-message-cancel">
      <@vrtx.csrfPreventionToken url />
      <input class="vrtx-button" type="submit" id="cancel" name="cancel" value="${vrtx.getMsg("editor.cancel")}" />
    </form>
    <#if !isCollection>
      <form action="" method="post" id="vrtx-message-delete">
        <@vrtx.csrfPreventionToken url />
        <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
        &nbsp;
        <input name="${url.path}" value="${url.path}" type="hidden" />
        <input class="vrtx-button" type="submit" name="delete" value="${vrtx.getMsg("tabMenuRight.deleteResourcesService")}" />
      </form>
    </#if>
  </#if>
</div>
</body>
</html>