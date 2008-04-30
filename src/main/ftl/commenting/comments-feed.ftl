<?xml version="1.0" encoding="utf-8"?>
<#import "/lib/vortikal.ftl" as vrtx />
<feed xmlns="http://www.w3.org/2005/Atom">
  <#--title type="html">${resource.title?html}</title-->
  <title type="html">
        <@vrtx.msg code='commenting.recentComments'
                   args=[resource.title] default='Recent comments' />
  </title>
  <link href="${urlMap[resource.URI]?html}" />
  <link rel="self" href="${selfURL?html}" />
  <#assign date_format>yyyy-MM-dd'T'HH:mm:ssZZ</#assign>
  <updated><@vrtx.date value=resource.lastModified format=date_format/></updated>
  <id>${selfURL?html}</id>
  <#list comments?reverse as comment>
  <#assign resource = resourceMap[comment.URI] />
  <entry>
    <title>Re: ${resource.title?html}</title>
    <link href="${(urlMap[resource.URI] + '#comment-' + comment.ID)?html}" />
    <id>${(urlMap[resource.URI] + '#comment-' + comment.ID)?html}</id>
    <author>
      <name>${comment.author.description?html}</name>
      <#if comment.author.URL?exists>
      <url>${comment.author.URL?html}</url>
      </#if>
    </author>
    <updated><@vrtx.date value=comment.time format=date_format /></updated>
    <summary type="html">${comment.content?html}</summary>
  </entry>
  </#list>
</feed>
