<#--
  - File: plain-header.ftl
  - 
  - Description: header that displays a breadcrumb and a resource title
  - 
  - Required model data:
  -   breadcrumb
  -
  - Optional model data:
  -   includeTemplate - a template location which is included if specified
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>
<#import "/lib/breadcrumb.ftl" as brdcrmb/>

<#if !breadcrumb?exists>
  <#stop "Unable to render model: required submodel
  'breadcrumb' missing">
</#if>

<@brdcrmb.breadCrumb crumbs=breadcrumb />
