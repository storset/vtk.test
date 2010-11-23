<#import "/lib/vortikal.ftl" as vrtx />

<#macro addFckScripts>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/imageref.js"></script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/serverbrowsedialog.js"></script>
  <script language="Javascript" type="text/javascript" src="${fckeditorBase.url?html}/ckeditor.js"></script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/admin-fck-setup.js"></script>
  <script type="text/javascript" src="${fckeditorBase.url?html}/adapters/jquery.js"></script>
  
</#macro>

<#macro insertEditor content completeEditor=false withoutSubSuper=false>
    <#local baseFolder = "/" />
    <#if resourceContext.parentURI?exists>
      <#local baseFolder = resourceContext.parentURI?html />
    </#if>

    <#include "../../lib/ckeditor/create-editor.ftl" />
</#macro>