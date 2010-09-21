<#ftl strip_whitespace=true>

<#--
  * ping
  * 
  * Description: Javascript for "pinging" a resource at
  * regular intervals, keeping the session alive, and possibly
  * performing other server-side tasks. Requires the XMLHttpRequest
  * Javascript object.
  * 
  * @param url the url to access 
  * @param interval the ping interval in seconds. The default value is
  *        600 (10 minutes)
  * @param method the request method (usually "HEAD" or "GET", the
  *        default is "GET" ("HEAD" causes Firefox to occasionally hang 
  *        waiting for the request to complete).
  *
  -->
<#macro ping url interval=600 method="GET">
  <script language="JavaScript" type="text/javascript"><!--
     var intervalSec = ${interval};
     var req;

     function ping() {
        if (req == null) {
           if (window.XMLHttpRequest) {
              req = new XMLHttpRequest();
           } else if (window.ActiveXObject) {
              req = new ActiveXObject("Microsoft.XMLHTTP");
           }
        }
        if (req != null) {
           req.open('${method}', '${url}', true);
           req.onreadystatechange = callback;
           req.send(null);
        }
     }

     function callback() {
        if (req != null && req.readyState == 4) {
              setTimeout('ping()', intervalSec * 1000);
        }
     }

     setTimeout('ping()', intervalSec * 1000);
  // -->
  </script>
</#macro>
