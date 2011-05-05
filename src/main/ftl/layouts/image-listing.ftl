<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/gallery.ftl" as gallery />

<#if !excludeScripts?exists>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
      <link rel="stylesheet" href="${cssURL}" />
    </#list>
  </#if>

  <#if jsURLs?exists && type == 'gallery'>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
</#if>

<#if images?exists>
  <#assign maxWidth = 507 />
  
  <div class="vrtx-image-listing-include">
    <span class="vrtx-image-listing-include-title"><a href="${folderUrl}?display=gallery">${folderTitle}</a></span>
    <#if type == 'gallery'>
      <@gallery.galleryJSInit maxWidth fadeEffect />
      <ul class="vrtx-image-listing-include-thumbs-pure-css">
    <#else>
      <ul class="vrtx-image-listing-include-thumbs">
    </#if>
        <@gallery.galleryListImages images />
      </ul>
  </div>
</#if>