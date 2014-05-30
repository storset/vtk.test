/*
 * Course schedule (Web Worker or runned in main thread)
 *
 * Performance credits:
 * http://stackoverflow.com/questions/5080028/what-is-the-most-efficient-way-to-concatenate-n-arrays-in-javascript
 * http://blog.rodneyrehm.de/archives/14-Sorting-Were-Doing-It-Wrong.html
 * (http://en.wikipedia.org/wiki/Schwartzian_transform)
 * http://jsperf.com/math-ceil-vs-bitwise/10
 */

this.onmessage = function(e) {
  postMessage(generateHTMLForType(e.data));
};

function scheduleUtils() {
  /** Private */
  var self = this,
  parseDate = function(dateString) {
    var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2})(?::([0-9]*)(\.[0-9]*)?)?(?:([+-])([0-9]{2}):([0-9]{2}))?/.exec(dateString);
    return { year: m[1], month: m[2], date: m[3], hh: m[4], mm: m[5], tzpm: m[8], tzhh: m[9], tzmm: m[10] };
  },
  getNowDate = new Date(),
  getDate = function(year, month, date, hh, mm, tzpm, tzhh, tzmm) {
    var date = new Date(year, month, date, hh, mm, 0, 0);
    
    var clientTimeZoneOffset = date.getTimezoneOffset();
    var serverTimeZoneOffset = (tzhh * 60) + tzmm;
    if(tzpm === "+") serverTimeZoneOffset = -serverTimeZoneOffset;
    
    if(clientTimeZoneOffset === serverTimeZoneOffset) return date; // Same offset in same date
    
    /* DST
    var isServerDateDst = tzhh === 1;
    var jan = new Date(date.getFullYear(), 0, 1);
    var jul = new Date(date.getFullYear(), 6, 1);
    var isClientDateDst = Math.max(jan.getTimezoneOffset(), jul.getTimezoneOffset()) > date.getTimezoneOffset(); */
    
    if(clientTimeZoneOffset > serverTimeZoneOffset) {
      var nd = new Date(date.getTime() + (clientTimeZoneOffset - serverTimeZoneOffset)); 
    } else {
      var nd = new Date(date.getTime() + (serverTimeZoneOffset - clientTimeZoneOffset));
    }
    return nd;
  },
  formatName = function(name) {
    var arr = name.replace(/ +(?= )/g, "").split(" ");
    var arrLen = arr.length;
    if(!arrLen) return name;
    
    var val = "";
    for(var i = 0, len = arrLen-1; i < len; i++) {
       val += arr[i].substring(0,1) + ". ";
    }
    return val + arr[i];
  },
  linkAbbr = function(url, title, text) {
    var val = "";
    if(url && title) {
      val += "<a title='" + title + "' href='" + url + "'>";
    } else if(url) {
      val += "<a href='" + url + "'>";
    } else if(title) {
      val += "<abbr title='" + title + "'>";
    }
    val += text;
    if(url) {
      val += "</a>";
    } else if(title) {
      val += "</abbr>";
    }
    return val;
  },
  jsonArrayToHtmlList = function(arr) {
    var val = "";
    var arrLen = arr.length;
    if(!arrLen) return val;
    
    if(arrLen > 1) val = "<ul>";
    for(var i = 0; i < arrLen; i++) {
      if(arrLen > 1) val += "<li>";
      var obj = arr[i];
      if(obj.name && obj.url) {
        val += "<a href='" + obj.url + "'>" + formatName(obj.name) + "</a>";
      } else if(obj.title && obj.url) {
        val += "<a href='" + obj.url + "'>" + obj.title + "</a>";
      } else if(obj.url) {
        val += "<a href='" + obj.url + "'>" + obj.url + "</a>";
      } else if(obj.name) {
        val += formatName(obj.name);
      } else if(obj.title) {
        val += obj.title;
      } else if(obj.id) {
        val += obj.id;
      }
      if(arrLen > 1) val += "</li>";
    }
    if(arrLen > 1) val += "</ul>";
    return val;
  },
  ceil = function(n) {
    var f = (n << 0),
    f = f == n ? f : f + 1;
    return f;
  };

  /** Public */
  this.nowDate = getNowDate; // Cache
  this.getDateTime = function(s, e) {
    var startDateTime = parseDate(s);
    var endDateTime = parseDate(e);
    return { start: startDateTime, end: endDateTime };
  };
  this.getDateFormatted = function(dateStart, dateEnd, i18n) {
    return parseInt(dateStart.date, 10) + ". " + i18n["m" + parseInt(dateStart.month, 10)].toLowerCase() + ".";
  },
  this.getEndDateDayFormatted = function(dateStart, dateEnd, i18n) {
    var endDate = getDate(dateEnd.year, parseInt(dateEnd.month, 10) - 1, parseInt(dateEnd.date, 10), parseInt(dateEnd.hh, 10), parseInt(dateEnd.mm, 10), dateEnd.tzpm, parseInt(dateEnd.tzhh, 10), parseInt(dateEnd.tzmm, 10));
    return { endDate: endDate, day: i18n["d" + endDate.getDay()] };
  };
  this.getTimeFormatted = function(dateStart, dateEnd) {
    return dateStart.hh + ":" + dateStart.mm + "&ndash;" + dateEnd.hh + ":" + dateEnd.mm;
  };
  this.getPostFixId = function(dateStart, dateEnd) {
    return dateStart.date + "-" + dateStart.month + "-" + dateStart.year + "-" + dateStart.hh + "-" + dateStart.mm + "-" + dateEnd.hh + "-" + dateEnd.mm;
  };
  this.getTitle = function(session, isCancelled, i18n) {
    return (isCancelled ? "<span class='course-schedule-table-status'>" + i18n.tableCancelled + "</span>" : "") + (session.vrtxTitle || session.title || session.id);
  };
  this.getPlace = function(session) {
    var val = "";
    var rooms = session.rooms;
    if(rooms && rooms.length) {
      var len = rooms.length;
      if(len > 1) val = "<ul>";
      for(var i = 0; i < len; i++) {
        if(len > 1) val += "<li>";
        var room = rooms[i]; 
        val += linkAbbr(room.buildingUrl, room.buildingName, (room.buildingAcronym || room.buildingId));
        val += " ";
        val += linkAbbr(room.roomUrl, room.roomName, room.roomId);
        if(len > 1) val += "</li>";
      }
      if(len > 1) val += "</ul>";
    }
    return val;
  };
  this.getStaff = function(session) {
    var val = "";
    var staff = session.vrtxStaff || session.staff || [];
    var externalStaff = session.vrtxStaffExternal;
    if(externalStaff && externalStaff.length) {
      staff.push.apply(staff, externalStaff);
    }
    return jsonArrayToHtmlList(staff);
  };
  this.getResources = function(session, fixedResources) {
    var resources = session.vrtxResources || [];
    if(fixedResources && fixedResources.length) {
      resources.push.apply(resources, fixedResources);
    }
    var val = jsonArrayToHtmlList(resources);
    var resourcesText = session.vrtxResourcesText;
    if(resourcesText && resourcesText.length) {
      val += resourcesText;
    }
    return val;
  };
  this.getTableStartHtml = function(activityId, caption, isAllPassed, hasResources, hasStaff, i18n) {
    var html = "<div tabindex='0' class='course-schedule-table-wrapper'>";
    html += "<table id='" + activityId + "' class='course-schedule-table uio-zebra hiding-passed" + (isAllPassed ? " all-passed" : "") + (hasResources ? " has-resources" : "")  + (hasStaff ? " has-staff" : "") + "'><caption>" + caption + "</caption><thead><tr>";
      html += "<th class='course-schedule-table-date'>" + i18n.tableDate + "</th>";
      html += "<th class='course-schedule-table-day'>" + i18n.tableDay + "</th>";
      html += "<th class='course-schedule-table-time'>" + i18n.tableTime + "</th>";
      html += "<th class='course-schedule-table-title'>" + i18n.tableTitle + "</th>";
      if(hasResources) html += "<th class='course-schedule-table-resources'>" + i18n.tableResources + "</th>";
      html += "<th class='course-schedule-table-place'>" + i18n.tablePlace + "</th>";
      if(hasStaff)     html += "<th class='course-schedule-table-staff'>" + i18n.tableStaff + "</th>";
    html += "</tr></thead><tbody>";
    return html;
  };
  this.getTableEndHtml = function(isNoPassed, i18n) {
    var html = "</tbody></table>";
    if(!isNoPassed) html += "<a class='course-schedule-table-toggle-passed' href='javascript:void(0);'>" + i18n.tableShowPassed + "</a>";
    html += "</div>";
    return html;
  };
  this.splitThirds = function(arr, title) {
    var html = "<span class='display-as-h3'>" + title + "</span>",
        len = arr.length,
        split1 = ceil(len / 3),
        split2 = split1 + ceil((len - split1) / 2);
    html += "<div class='course-schedule-toc-thirds'><ul class='thirds-left'>";
    for(var i = 0; i < len; i++) {
      if(i === split1) html += "</ul><ul class='thirds-middle'>";
      if(i === split2) html += "</ul><ul class='thirds-right'>";
      html += arr[i];
    }
    html += "</ul></div>";
    return html;
  };
  this.editLink = function(clazz, html, displayEditLink, canEdit, i18n) {
    var startHtml = "<td class='" + clazz + ((displayEditLink && canEdit) ? " course-schedule-table-edit-cell" : "") + "'>";
    var endHtml = "</td>"
    if(!displayEditLink || !canEdit) return startHtml + html + endHtml;

    return startHtml + "<div class='course-schedule-table-edit-wrapper'>" + html + "<a class='button course-schedule-table-edit-link' href='javascript:void'><span>" + i18n.tableEdit + "</span></a></div>" + endHtml;
  };
}

function generateHTMLForType(d) {
  var dta = JSON.parse(d),
      data = dta.data["activities"],
      tocHtml = "",
      tablesHtml = "";
  
  if(!data) return { tocHtml: tocHtml, tablesHtml: tablesHtml };
  var dataLen = data.length;
  if(!dataLen) return { tocHtml: tocHtml, tablesHtml: tablesHtml };
  
  var type = dta.type,
      scheduleI18n = dta.i18n,
      canEdit = dta.canEdit,
      skipTier = type === "plenary",
      startGenHtmlForTypeTime = new Date();
      
  var utils = new scheduleUtils(), // Cache
      nowDate = utils.nowDate,
      getDateTime = utils.getDateTime,
      getDateFormatted = utils.getDateFormatted,
      getEndDateDayFormatted = utils.getEndDateDayFormatted,
      getTimeFormatted = utils.getTimeFormatted,
      getPostFixId = utils.getPostFixId,
      getTitle = utils.getTitle,
      getPlace = utils.getPlace,
      getStaff = utils.getStaff,
      getResources = utils.getResources,
      getTableStartHtml = utils.getTableStartHtml,
      getTableEndHtml = utils.getTableEndHtml,
      splitThirds = utils.splitThirds,
      editLink = utils.editLink;

  var lastDtShort = "",
      forCode = "for",
      sequences = {}, // For fixed resources
      tocTimeMax = 3,
      tocHtmlArr = [];
  
  tocHtml += "<h2 class='course-schedule-toc-title accordion'>" + scheduleI18n["header-" + type] + "</h2>";
  tocHtml += "<div class='course-schedule-toc-content'>";
  if(skipTier) tocHtml += "<ul>";
  
  for(var i = 0; i < dataLen; i++) {
    var dt = data[i];
    
    var id = dt.id;
    var dtShort = dt.teachingMethod.toLowerCase();
    var dtLong = dt.teachingMethodName;
    var isFor = dtShort === forCode;
    
    if(!isFor || i == 0) {
      var activityId = isFor ? dtShort : dtShort + "-" + dt.id;
      var sessionsHtml = "";
      var resourcesCount = 0;
      var staffCount = 0;
      var passedCount = 0;
      var sessionsCount = 0;
      var sessions = [];
      var sessionsPreprocessed = [];
      if(skipTier) {
        var caption = dtLong;
      } else {
        var groupCount = id.split("-")[1];
        var caption = dtLong + " - " + scheduleI18n.groupTitle.toLowerCase() + " " + groupCount;
      }
      var tocTime = "";
      var tocTimeCount = 0;
    }
    
    // Add together sessions from sequences
    for(var j = 0, len = dt.sequences.length; j < len; j++) {
      var sequence = dt.sequences[j];
      var fixedResources = sequence.vrtxResourcesFixed;
      if(fixedResources && fixedResources.length) {
        sequences[sequence.id] = fixedResources;
        resourcesCount++;
      }
      sessions.push.apply(sessions, sequence.sessions);
    }

    if(!isFor || (isFor && (!data[i+1] || data[i+1].teachingMethod.toLowerCase() !== forCode))) {

      // Evaluate and cache dateTime, staff and resources
      var map = [];
      for(j = 0, len = sessions.length; j < len; j++) {
        var session = sessions[j];
        var dateTimeA = getDateTime(session.dtStart, session.dtEnd);
        var startA = dateTimeA.start;
        var endA = dateTimeA.end;
        var a = startA.year + "" + startA.month + "" + startA.date + "" + startA.hh + "" + startA.mm + "" + endA.hh + "" + endA.mm;
        var sequenceId = session.id.replace(/\/[^\/]*$/, "");
        var staff = getStaff(session);
        if(staff.length) staffCount++;
        var resources = getResources(session, (sequences[sequenceId] || null));
        if(resourcesCount.length) resourcesCount++;
        map.push({
          "index": j, // Save index
          "value": a,
          "dateTime": dateTimeA,
          "staff": staff,
          "resources": resources
        });
      }
      // Sort
      map.sort(function(a, b) {
        return a.value > b.value ? 1 : -1;
      });
      // Generate sessions HTML (get correctly sorted from map)
      for(j = 0, len = map.length; j < len; j++) {
        var sessionPreprocessed = map[j];
        var session = sessions[sessionPreprocessed.index];
        
        var dateTime = sessionPreprocessed.dateTime;
        var date = getDateFormatted(dateTime.start, dateTime.end, scheduleI18n);
        var endDateDay = getEndDateDayFormatted(dateTime.start, dateTime.end, scheduleI18n);
        var endDate = endDateDay.endDate;
        var day = endDateDay.day;
        var time = getTimeFormatted(dateTime.start, dateTime.end);
        
        var sessionId = (skipTier ? type : dtShort + "-" + id) + "-" + session.id.replace(/\//g, "-") + "-" + getPostFixId(dateTime.start, dateTime.end);
        var isCancelled = (session.status && session.status === "cancelled") ||
                          (session.vrtxStatus && session.vrtxStatus === "cancelled");

        var classes = (j & 1) ? "even" : "odd";     
        if(isCancelled) {
          if(classes !== "") classes += " ";
          classes += "cancelled";
        }
        if(endDate <= nowDate) {
          if(classes !== "") classes += " ";
          classes += "passed";
          passedCount++;
        }
        sessionsCount++;
        
        sessionsHtml += classes !== "" ? "<tr tabindex='0' id='" + sessionId + "' class='" + classes + "'>" : "<tr>";
          sessionsHtml += "<td class='course-schedule-table-date'>" + date + "</td>";
          sessionsHtml += "<td class='course-schedule-table-day'>" + day + "</td>";
          sessionsHtml += "<td class='course-schedule-table-time'>" + time + "</td>";
          sessionsHtml += "<td class='course-schedule-table-title'>" + getTitle(session, isCancelled, scheduleI18n) + "</td>";
          if(resourcesCount) sessionsHtml += "<td class='course-schedule-table-resources'>" + sessionPreprocessed.resources + "</td>";
          sessionsHtml += editLink("course-schedule-table-place", getPlace(session), !staffCount, canEdit, scheduleI18n);
          if(staffCount)     sessionsHtml += editLink("course-schedule-table-staff", sessionPreprocessed.staff, staffCount, canEdit, scheduleI18n);
        sessionsHtml += "</tr>";
      
        if(tocTimeCount < tocTimeMax) {
          var newTocTime = day.toLowerCase().substring(0,3) + " " + time;
          if(tocTime.indexOf(newTocTime) === -1) {
            if(tocTimeCount > 0) {
              tocTime += ", ";
              tocTime += "<span>";
            }
            tocTime += newTocTime;
            if(tocTimeCount === 0) tocTime += "</span>";
            tocTimeCount++;
          }
        }
      }
      tablesHtml += getTableStartHtml(activityId, caption, (passedCount === sessionsCount), resourcesCount, staffCount, scheduleI18n) + sessionsHtml + getTableEndHtml(passedCount === 0, scheduleI18n);
      
      // Generate ToC
      tocTime = tocTime.replace(/,([^,]+)$/, " " + scheduleI18n.and + "$1");
      if(skipTier) {
        tocHtml += "<li><span><a href='#" + activityId + "'>" + dtLong + "</a> - " + tocTime + "</li>";
      } else {
        tocHtmlArr.push("<li><span><a href='#" + activityId + "'>" + scheduleI18n.groupTitle + " " + groupCount + "</a> - " + tocTime + "</li>");
        if((dtShort !== lastDtShort && i > 0) || (i === (dataLen - 1))) {
          tocHtml += splitThirds(tocHtmlArr, dtLong);
          tocHtmlArr = [];
        }
      }
    }
    
    lastDtShort = dtShort;
  }
  
  if(skipTier) tocHtml += "</ul>";
  tocHtml += "</div>";
  
  return { tocHtml: tocHtml, tablesHtml: tablesHtml, time: (+new Date() - startGenHtmlForTypeTime) };
}