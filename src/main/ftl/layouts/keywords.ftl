<#ftl strip_whitespace=true>
<#--
  - File: keywords.ftl
  - 
  - Description: displays the single or list of links
  - 
  - Required model data:
  -     urls - a list of urls
  -     values - a list of values
  - 
  -->

<#import "/lib/vtk.ftl" as vrtx/>

<#if values?exists>
  <span class="vrtx-tags">
    <#if title?exists>
      <span class="title">${title?html}:</span>
    <#else>
      <span class="title"><@vrtx.msg code="decorating.tags" default="Tags" />:</span>
    </#if>
    <span class="vrtx-tags-links">
       <#list values as v>
         <a href="${urls[v_index]?html}">${v?html}</a><#if v_index &lt; values?size - 1>,<#t/></#if>
       </#list>
    </span>
  </span>
</#if>
