<#macro printPropertyEditView title inputFieldName description value="true" tooltip="" classes="">
<#assign locale = springMacroRequestContext.getLocale() />
<div class="vrtx-radio ${classes}">
  <div><label for="${inputFieldName}">${title}</label></div>
  <div>
      <input name="${inputFieldName}" id="${inputFieldName}-true" type="radio" value="true" <#if value == "true"> checked="checked" </#if> />
      <label for="${inputFieldName}-true">${description.getVocabulary(locale,"true")}</label> 
      <input name="${inputFieldName}" id="${inputFieldName}-false" type="radio" value="false" <#if value != "true"> checked="checked" </#if> />
      <label for="${inputFieldName}-false">${description.getVocabulary(locale,"false")}</label> 
  </div>
</div>
</#macro>