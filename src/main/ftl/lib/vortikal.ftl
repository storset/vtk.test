<#import "/spring.ftl" as spring />

<#--
 * vortikal.ftl
 *
 * (Will become) a collection of useful FreeMarker macros and functions :)
 *
 * The  "exposeSpringMacroHelpers" property on the spring FreeMarker
 * configuration must be set.
 *
 -->

<#if !springMacroRequestContext?exists>
  <#stop "springMacroRequestContext must be exposed. See the
         'exposeSpringMacroHelpers' property of the FreeMarker
         configuration." />
</#if>

<#--
 * msg
 *
 * Get a localized, html-escaped message from the spring RequestContext. 
 * Example: <@vrtx.msg code="my.message.key" default="My default message" args="['my param']"
 *
 * @param code the code of the localized message
 * @param default (optional - set to code if not specified) the default message if the localized message did not
 *        exist for the currently selected locale.
 * @param args (optional) arguments for the message
 *
-->
<#macro msg code default=code args=[] >
  <#compress>
    <#local localizer =
    "org.vortikal.web.view.freemarker.MessageLocalizer"?new(code, default, args, springMacroRequestContext) />
    ${localizer.msg?html}
  </#compress>
</#macro>



<#--
 * rawMsg
 *
 * Get a localized, unescaped message from the spring RequestContext. 
 * Example: <@vrtx.rawMsg code="my.message.key" default="My default message" args="['my param']"
 *
 * @param code the code of the localized message
 * @param default the default message if the localized message did not
 *        exist for the currently selected locale.
 * @param args (optional) arguments for the message
 *
-->
<#macro rawMsg code default args=[] >
  <#compress>
    <#local localizer =
    "org.vortikal.web.view.freemarker.MessageLocalizer"?new(code, default, args, springMacroRequestContext) />
  ${localizer.msg}
  </#compress>
</#macro>


<#--
 * getMsg
 *
 * Same as the macro 'rawMsg', but returns a value instead of printing it.
 * Example: <#assign msg = vrtx.getMsg("my.message.key", "My default message", ['my param']) />
 *
 * @param code the code of the localized message
 * @param default the default message if the localized message did not
 *        exist for the currently selected locale.
 * @param args (optional) arguments for the message
 *
-->
<#function getMsg code default args=[] >
    <#assign localizer =
    "org.vortikal.web.view.freemarker.MessageLocalizer"?new(code, default, args, springMacroRequestContext) />
  <#return localizer.msg/>
</#function>



<#-- REWORKED SPRINGS VERSION
 * 
 * formRadioButtons
 *
 * Show radio buttons.
 *
 * @param path the name of the field to bind to
 * @param options a map (value=label) of all the available options
 * @param separator the html tag or other character list that should be used to
 *        separate each option.  Typically '&nbsp;' or '<br>'
 * @param attributes any additional attributes for the element (such as class
 *        or CSS styles or size
-->

<#-- FIXME: Only works for CreateDocument -->
<#macro formRadioButtons path options pre post attributes="">
	<@spring.bind path/>
	<#list options?keys as value>${pre}
	<input type="radio" name="${spring.status.expression}" id="${value}" value="${value}"
	  <#if spring.status.value?default("") == value>checked="checked"</#if> onclick="javascript:changetemplatename('${options[value]}')" ${attributes}
	<@spring.closeTag/><label for="${value}">${options[value]}</label>
	${post}
	</#list>
</#macro>


<#--
 * date
 *
 * Get a localized, formatted date string from a date object.
 * Examples: <@vrtx.date value=my.date.object format='short' />
 *           <@vrtx.date value=my.date.object format='yyyy-MM-dddd HH:mm:ss' />
 *
 * @param value the date object
 * @param format a named format, or a java DateFormat
 *        string. See org.vortikal.repository.resourcetype.ValueFormatter
 *
-->
<#macro date value format>
  <#compress>
    <#if VRTX_VALUE_FORMATTER?exists>
      <#local constructor = "freemarker.template.utility.ObjectConstructor"?new() />
      <#local val = constructor("org.vortikal.repository.resourcetype.Value", value, false) />
      <#local locale = springMacroRequestContext.getLocale() />
      ${VRTX_VALUE_FORMATTER.valueToString(val, format, locale)}
    <#else>
      Undefined
    </#if>
  </#compress>
</#macro>



<#macro property resource name prefix="" >
  <#compress>
    <#if VRTX_RESOURCE_TYPE_TREE?exists>
      <#if prefix == "">
        <#if VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(nullArg, name)?exists>
          <#local def = VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(nullArg, name) />
          <#if resource.getProperty(def)?exists>
            ${resource.getProperty(def).getValue()}
          </#if>
        </#if>
      <#else>
        <#if VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(prefix, name)??>
          <#local def = VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(prefix, name) />
          <#if resource.getProperty(def)?exists>
            ${resource.getProperty(def).getValue()}
          </#if>
        </#if>
      </#if>
    </#if>
  </#compress>
</#macro>
