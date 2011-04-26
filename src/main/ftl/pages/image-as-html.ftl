<#import "/lib/vortikal.ftl" as vrtx />
<#assign lang><@vrtx.requestLanguage/></#assign>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
</head>
<body id="vrtx-image-preview">

<h1>${title}</h1>

<#if src?exists>
  <div class="vrtx-introduction-image">
    <img src="${src}" alt="image" />
    <#assign photographer = vrtx.propValue(resource, "photographer") />
    <#if photographer?exists && photographer != "">
	  <div class="vrtx-imagetext">
        <span class="vrtx-photo">
          <span class="vrtx-photo-prefix">${vrtx.getMsg('imageAsHtml.byline')}: </span>${photographer}
        </span>
      </div> 
    </#if>
  </div>
</#if>

<h2>${vrtx.getMsg('imageAsHtml.source')}</h2>

<#assign pixelHeight = vrtx.propValue(resource, "pixelHeight") />
<#assign pixelWidth = vrtx.propValue(resource, "pixelWidth") />

<a href="${src}">${src}</a> (${pixelHeight}px x ${pixelWidth}px)

<#if description?exists >
  <h2>${vrtx.getMsg('imageAsHtml.description')}</h2>
  <div id="vrtx-meta-description">
    ${description}
  </div>
</#if>

<#assign url = .vars["uioWebCopyrightURL." + lang] />
<#if url?exists>
  <h2>${vrtx.getMsg('imageAsHtml.usage')}</h2>
  <a href="${url?html}">${vrtx.getMsg('imageAsHtml.rights')}</a>
</#if>

</body>
</html>