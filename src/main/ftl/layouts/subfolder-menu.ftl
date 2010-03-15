<#--
  - File: subfolder-menu.ftl
  - 
  - Description: Sub folder menu implementation
  - 
  - Required model data:
  -   subFolderMenu
  -
  -->
<#import "../lib/vortikal.ftl" as vrtx />
 <#-- RECURSIVE MENU BUILD --> 

<#if subFolderMenu?exists> 
	<@displaySubFolderMenu subFolderMenu false />
</#if>

<#global USE_TREE_VIEW = false >

<#macro displaySubFolderMenu subFolderMenu treeView>
	<#assign USE_TREE_VIEW=treeView>
	
    <#if subFolderMenu.size &gt; 0>
      <#assign "counter" = 0>
      <#assign "counter2" = 0>
      <#assign "c" = 0>
         
      <#if subFolderMenu.resultSets?exists>
        <div class="vrtx-subfolder-menu">
          <#if subFolderMenu.title?exists>
            <div class="menu-title">${subFolderMenu.title?html}</div>
          </#if>
          <#list subFolderMenu.resultSets as resultSet>
            <#assign counter = counter+1>
            <#assign counter2 = counter2+1>
            <#if subFolderMenu.groupResultSetsBy?exists && (subFolderMenu.groupResultSetsBy?number = counter2 || counter = 1)>
               <#assign "counter2" = 0>
               <#assign c = c+1>
               <@displayParentMenu menu=resultSet currentCount=counter groupCount=c newDiv=true subFolderMenu=subFolderMenu />
            <#else>
               <@displayParentMenu menu=resultSet currentCount=counter groupCount=c newDiv=false subFolderMenu=subFolderMenu />
            </#if>
          </#list>
        </div>
      </#if>
</#if>

</#macro>

<#-- MACROS: -->
<#macro displayItem item >
  <#if item.menu?exists>
    <a href="${item.url?html}">${item.label}</a>
    <#if USE_TREE_VIEW>
       </span>
     </#if>
    <@displaySubMenu item.menu displaySubMenu />
  <#else>
    <a href="${item.url?html}">${item.label}</a>
  </#if>
</#macro>

<#macro displayParentMenu menu currentCount groupCount newDiv subFolderMenu >
  <#if newDiv>
       <#if currentCount != 1>
          </div>
       </#if>  
          <div class="vrtx-group-${groupCount?html}">
  </#if>
   <#if USE_TREE_VIEW >
    <ul class="resultset-${currentCount?html} filetree">
   <#else>
    <ul class="resultset-${currentCount?html}">
   </#if>
    <#list menu.itemsSorted as item>
      <li>
        <#if USE_TREE_VIEW >       
          <span class="folder">
        </#if>
        <@displayItem item=item />
      </li>
    </#list>
  </ul>
  
  <#if subFolderMenu.groupResultSetsBy?exists && subFolderMenu.groupResultSetsBy?number &gt; 0 && currentCount == subFolderMenu.resultSets?size>
     </div>
  </#if>
</#macro>

<#macro displaySubMenu menu displaySubMenu >
  <ul>
  	<#assign i = 0 />
    <#list menu.itemsSorted as item>
    	<#if (i < menu.maxNumberOfItems)>
	        <#if USE_TREE_VIEW >
	          <li class="closed">
	          <span class="folder">
	        <#else>
	          <li>
	        </#if> 
            <@displayItem item=item />
          	</li>
         <#else>
         	<#break>
         </#if>
         <#assign i = i + 1 />
    </#list>
	<#if (menu.totalNumberOfItems > menu.maxNumberOfItems)>
	    <li class="vrtx-more">   
			<a href="${menu.moreUrl?html}"><@vrtx.msg code="subfolder.morelinkTitle" /></a>
		</li>
	</#if>
  </ul>
</#macro>