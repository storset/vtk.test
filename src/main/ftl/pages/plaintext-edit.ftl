<#ftl strip_whitespace=true>

<#--
  - File: plaintext-edit.ftl
  - 
  - Description: HTML page that displays a form for editing the
  - contents of a plain-text resource
  - 
  - Required model data:
  -  pingURL
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/ping.ftl" as ping />
<#if !plaintextEditForm?exists>
  <#stop "Unable to render model: required submodel
  'plaintextEditForm' missing">
</#if>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Plain text edit</title>
  <script type="text/javascript" language="Javascript" src="${md5jsURL?html}"></script>
  <@ping.ping url=pingURL['url'] interval=300/>
  <script type="text/javascript" language="Javascript"><!--

    var before = null;
    var saveButton = false;

    function checkSubmit() {
       saveButton = true;
       return true;
    }

     window.onload = function() {
        before = hex_md5(document.getElementById("foo").value);
     }

     window.onbeforeunload = function() {
        if (saveButton) return;

        var now = hex_md5(document.getElementById("foo").value);
        if (before == now) {
           return;
        }
        return '<@vrtx.msg code='manage.unsavedChangesConfirmation' />';
     }
    // -->
  </script>

</head>
<body>
  <div style="width:99%;">
    <form action="${plaintextEditForm.submitURL}" method="POST">
      <textarea style="width:100%;" id="foo" name="content" rows="30" cols="80">${plaintextEditForm.content?html}</textarea>
      <div style="padding-top:7px;">
        <input type="submit" id="saveQuitAction" name="saveQuitAction" onclick="checkSubmit()" value="<@vrtx.msg code="plaintextEditForm.saveAndQuit" default="Save and quit"/>">
        <input type="submit" id="saveAction" name="saveAction" onclick="checkSubmit()" value="<@vrtx.msg code="plaintextEditForm.save" default="Save"/>">
        <input type="submit" id="cancelAction" name="cancelAction" value="<@vrtx.msg code="plaintextEditForm.cancel" default="Cancel"/>">
        <#if plaintextEditForm.tooltips?exists>
          <#list plaintextEditForm.tooltips as tooltip>
           <div class="contextual-help"><a href="javascript:void(0);" onclick="javascript:open('${tooltip.url?html}', 'componentList', 'width=650,height=450,resizable=yes,right=0,top=0,screenX=0,screenY=0,scrollbars=yes');">
              <@vrtx.msg code=tooltip.messageKey default=tooltip.messageKey/>
            </a>
           </div>
          </#list>
        </#if>
      </div>
    </form>
  </div>
</body>
</html>
