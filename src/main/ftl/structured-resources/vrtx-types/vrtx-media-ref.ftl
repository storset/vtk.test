<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName tooltip="" classes="" value="" baseFolder="/">
  <div class="vrtx-media-ref ${classes}">
    <div>
      <label for="${inputFieldName}">${title}</label>
    </div>
    <div>
	  <input class="vrtx-textfield" type="text" id="${inputFieldName}" name="${inputFieldName}" value="${value?html}" size="30"/>
	  <div class="vrtx-button">
        <button type="button" onclick="browseServer('${inputFieldName}', '${fckeditorBase.url?html}', '${baseFolder}','${fckBrowse.url.pathRepresentation}','Media');"><@vrtx.msg code="editor.browseMediaFiles"/></button>
	  </div>
    </div>
  </div>
</#macro>
