<#ftl strip_whitespace=true>

<#import "/lib/view-collectionlisting.ftl" as coll />
<#import "/lib/collections/view-project-listing.ftl" as projects />
<#import "/lib/collections/view-person-listing.ftl" as persons />

<#macro displayTagElements tagElements>
  <div id="vrtx-tags-service">
    <ul class="vrtx-tag">
    <#assign i = 0>
    <#list tagElements as element>
      <li class="vrtx-tags-element-${i}">
        <a class="tags" href="${element.linkUrl?html}" rel="tags">${element.text?html}</a>
      </li>
  <#assign i = i+1>
    </#list>
    </ul>
  </div>
</#macro>

<#macro displayTagListing listing>
  
  <div class="tagged-resources vrtx-resources">
    <#if resourceType?exists>
      <#if resourceType = 'person'>
        <@persons.displayPersons listing />
      <#elseif resourceType = 'structured-project'>
        <@projects.displayProjects listing />
      <#else>
        
        <#-- TODO handle article and event listings -->
        
        <@displayCommonTagListing listing />
      </#if>
    <#else>
      <@displayCommonTagListing listing />
    </#if>
  </div>
  
  <div class="vrtx-paging-feed-wrapper">
    <#if pageThroughUrls?exists && (pageThroughUrls?size > 1) >
      <span class="vrtx-paging-wrapper"> 
      <#if (page-2 > -1) >
        <a class="vrtx-previous" href="${pageThroughUrls[page-2]}"><@vrtx.msg code="viewCollectionListing.previous" /></a>
      </#if>
      <#list pageThroughUrls as url>
        <a href="${url?html}" class="vrtx-page-number <#if (url_index+1) = page>vrtx-marked</#if>">${(url_index+1)}</a>
      </#list>
      <#if (pageThroughUrls?size > page) >
        <a class="vrtx-next" href="${pageThroughUrls[page]}"><@vrtx.msg code="viewCollectionListing.next" /></a>
      </#if>
      </span>
    </#if>
    
    <#if alternativeRepresentations?exists>
      <#list alternativeRepresentations as alt>
        <#if alt.contentType = 'application/atom+xml'>
          <#assign altUrl = alt.url />
          <#if resourceTypes?exists>
            <#list resourceTypes as resTyp>
              <#assign altUrl = altUrl + "&resource-type=" + resTyp.name />
            </#list>
          </#if>
          <div class="vrtx-feed-link">
            <a id="vrtx-feed-link" href="${altUrl?html}"><@vrtx.msg code="viewCollectionListing.feed.fromThis" /></a>
          </div>
          <#break />
        </#if>
      </#list>
     </#if>
  </div>
  
</#macro>

<#macro displayCommonTagListing listing>
  
  <#assign resources=listing.getFiles() />
  <#assign urls=listing.urls />
  <#assign displayPropDefs=listing.displayPropDefs />
  <#assign i = 1 />

  <#list resources as resource>
    <#assign resourceTitle = vrtx.prop(resource, "title", "").getFormattedValue() />
    <#assign introImageProp = vrtx.prop(resource, "picture", "")?default("") />
    <div class="vrtx-resource" id="vrtx-result-${i}">
      <#if introImageProp != "">
      <a href="${resource.getURI()?html}" class="vrtx-image">
        <#assign src = vrtx.propValue(resource, 'picture', 'thumbnail') /><img src="${src?html}" />
      </a>
      </#if>
      <div class="vrtx-title">
        <a href="${resource.getURI()?html}" class="vrtx-title"> ${resourceTitle?html}</a>
      </div>
      <#list displayPropDefs as displayPropDef>
        <#if displayPropDef.name = 'introduction'>
          <#assign val = vrtx.getIntroduction(resource) />
        <#elseif displayPropDef.type = 'IMAGE_REF'>
          <#assign val><img src="${vrtx.propValue(resource, displayPropDef.name, "")}" /></#assign>
        <#elseif displayPropDef.name = 'lastModified'>
          <#assign val>
            <@vrtx.msg code="viewCollectionListing.lastModified" args=[vrtx.propValue(resource, displayPropDef.name, "long")] />
          </#assign>
        <#else>
          <#assign val = vrtx.propValue(resource, displayPropDef.name, "long") />
        </#if>
        <#if val?has_content>
          <div class="${displayPropDef.name}">
            ${val} 
            <#if displayPropDef.name = 'introduction'>
              <#assign hasBody = vrtx.propValue(resource, 'hasBodyContent') == 'true' />
              <#if hasBody>
                <div class="vrtx-read-more">
                  <a href="${listing.urls[resource.URI]?html}" class="more">
                    <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
                  </a>
                </div>
              </#if>
            </#if>
          </div>
        </#if> 
      </#list>
    </div>
    <#assign i = i + 1 />
  </#list>
  
</#macro>