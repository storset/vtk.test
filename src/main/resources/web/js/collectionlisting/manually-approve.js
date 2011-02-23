/**
 * JS for handling manually approved resources
 */

function toggleManuallyApprovedContainer(resources) {
  // Toggle visibility of container for list of resources to manually approve.
  // "resources" is complete list (array) of resources. If empty, hide
  // container, else display.
  // "manuallyApproved" contains list of resources already manually
  // approved. Use to mark resources.

  // TODO: i18n ++
  // TODO: hmmm.. Too slow with 51s in IE 7 and 2s in Chrome when 3000 resources
  //       Need either to create all tables and add rows when next/prev paging (pri),
  //       or use setTimeout loop on all pages after first page

  var pages = 1, prPage = 25, len = resources.length;
  var totalPages = len > prPage ? (parseInt(len / prPage) + 1) : 1;
  
  var html = "<div id='approve-page-" + pages + "'>"
           + "<table><thead><tr><th>Tittel</th><th>Uri</th><th>Publisert</th></thead></tr><tbody>";
  
  for(var i = 0; i < len; i++) {
    if(resources[i].approved) {
      html += "<tr><td><input type='checkbox' checked='checked' />";
    } else {
      html += "<tr><td><input type='checkbox' />";
    }
    html += "<a href='" + resources[i].uri + "'>" + resources[i].title + "</a></td>"
          + "<td>" + resources[i].uri + "</td><td>" + resources[i].published + "</td></tr>";
    if((i+1) % prPage == 0) {
      html += "</tbody></table>";
      html += "<span class='approve-info'>Viser " + (((pages-1) * prPage)+1) + "-" + (pages * prPage) + " av " + len + "</span>";
      pages++;
      if(i < len-1) {
        if(i > prPage) {
          html += "<a href='#page-" + (pages-2) + "' class='prev' id='page-" + (pages-2) + "'>Forrige " + prPage + "</a>";
        }
        var nextPrPage = pages < totalPages || len % prPage == 0 ? prPage : len % prPage;
        html += "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>Neste " + nextPrPage + "</a>"
              + "</div><div id='approve-page-" + pages + "'>"
              + "<table><thead><tr><th>Tittel</th><th>Uri</th><th>Publisert</th></tr></thead><tbody>";
      }
    }
  }
  if(len % prPage != 0) {
    html += "</tbody></table><span class='approve-info'>Viser " + (((pages-1) * prPage)+1) + "-" + len + " av " + len + "</span>";
  }
  if(len > prPage) {
    html += "<a href='#page-" + (pages-1) + "' class='prev' id='page-" + (pages-1) + "'>Forrige " + prPage + "</a>";
  }
  html += "</div>";

  $("#manually-approve-container").html(html);
  $("#manually-approve-container div").not("#approve-page-1").hide();

}

function retrieveResources(serviceUri) {
  // Retrieve and return array of resources for folders to manually approve from.
  // Needs Vortex-service.
	
  // Dummy JSON
  var manyResources = [], rnd, app;
  for (var i = 0; i < 108; i++) {
	app = Math.random() > 0.5 ? true : false;
    manyResources.push({title: "Lorem ipsum dolore", uri: "/om/dokument.html", published: "2011-11-18", approved: app});
  }
  return manyResources;
}

$(document).ready(function() {

    var folders = $("#resource\\.manually-approve-from").val().split(",");
    var resources = retrieveResources(folders, "resource");
    toggleManuallyApprovedContainer(resources);
	
    // Refresh table
    $("#manually-approve-refresh").click(function(e) {
      var folders = $("#resource\\.manually-approve-from").val().split(",");
      var resources = retrieveResources(folders, "resource");
      toggleManuallyApprovedContainer(resources);
      return false; 
    });

    // Paging - Next
    $("#manually-approve-container").delegate(".next", "click", function() {
      var that = $(this).parent();
      var next = that.next();
      if(next) {
         $(that).hide();
         $(next).show();
      }
      return false;
    });

    // Paging - Previous
    $("#manually-approve-container").delegate(".prev", "click", function() {
      var that = $(this).parent();
      var prev = that.prev();
      if(prev) {
         $(that).hide();
         $(prev).show();
      }
      return false;
    });
 });