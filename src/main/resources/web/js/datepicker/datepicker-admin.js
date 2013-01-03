/*
 * Datepicker for new documenttypes
 *
 */

var DATE_PICKER_INITIALIZED = $.Deferred();
function initDatePicker(language) {
  var contents = $("#contents");

  // i18n (default english)
  if (language == 'no') {
    $.datepicker.setDefaults($.datepicker.regional['no']);
  } else if (language == 'nn') {
    $.datepicker.setDefaults($.datepicker.regional['nn']);
  }
  
  var dateFields = contents.find(".date");
  for(var i = 0, len = dateFields.length; i < len; i++) {
    displayDateAsMultipleInputFields(dateFields[i].name);
  }
  
  // Help user with time
  contents.on("change", ".vrtx-hours input", function () {
    var hh = $(this);
    var mm = hh.parent().nextAll(".vrtx-minutes").find("input"); // Relative to
    timeHelp(hh, mm);
  });
  contents.on("change", ".vrtx-minutes input", function () {
    var mm = $(this);
    var hh = mm.parent().prevAll(".vrtx-hours").find("input"); // Relative to
    timeHelp(hh, mm);
  });
  
  // Specific for start and end date
  var startDateElm = contents.find("#start-date-date");
  var endDateElm = contents.find("#end-date-date");
  
  if (!startDateElm.length || !endDateElm.length) {
    DATE_PICKER_INITIALIZED.resolve();
    return;
  }
  if (startDateElm.datepicker('getDate') != null) {
    setDefaultEndDate(startDateElm, endDateElm);
  }
  
  contents.on("change", "#start-date-date, #end-date-date", function () {
    setDefaultEndDate(startDateElm, endDateElm);
  });
  
  DATE_PICKER_INITIALIZED.resolve();
}

function displayDateAsMultipleInputFields(name) {
  var hours = "";
  var minutes = "";
  var date = [];
  var fieldName = name.replace(/\./g, '\\.');

  var elem = $("#" + fieldName);

  if (elem.length) {
    hours = extractHoursFromDate(elem[0].value);
    minutes = extractMinutesFromDate(elem[0].value)
    date = new String(elem[0].value).split(" ");
  }

  var dateField = "<div class='vrtx-textfield vrtx-date'><input type='text' size='12' id='" + name + "-date' value='" + date[0] + "' /></div>";
  var hoursField = "<div class='vrtx-textfield vrtx-hours'><input type='text' size='2' id='" + name + "-hours' value='" + hours + "' /></div>";
  var minutesField = "<div class='vrtx-textfield vrtx-minutes'><input type='text' size='2' id='" + name + "-minutes' value='" + minutes + "' /></div>";
  elem.parent().hide();
  elem.parent().after(dateField + hoursField + "<span class='vrtx-time-seperator'>:</span>" + minutesField);
  $("#" + fieldName + "-date").datepicker({
    dateFormat: 'yy-mm-dd'
  });
}

function setDefaultEndDate(startDateElm, endDateElm) {
  var endDate = endDateElm.val();
  var startDate = startDateElm.datepicker('getDate');
  if (endDate == "") {
    endDateElm.datepicker('option', 'defaultDate', startDate);
  }
}


function timeHelp(hh, mm) {
  var hhVal = hh.val();
  var mmVal = mm.val();
  if(hhVal.length || mmVal.length) {
    var newHhVal = parseInt(hhVal); // Correct hours
    if(isNaN(newHhVal)) {
      newHhVal = "00";
    } else {
      newHhVal = (newHhVal > 23) ? "00" : newHhVal;
      newHhVal = ((newHhVal < 10 && !newHhVal.length) ? "0" : "") + newHhVal;
    }
    var newMmVal = parseInt(mmVal); // Correct minutes
    if(isNaN(newMmVal)) {
      newMmVal = "00";
    } else {
      newMmVal = (newMmVal > 59) ? "00" : newMmVal;
      newMmVal = ((newMmVal < 10 && !newMmVal.length) ? "0" : "") + newMmVal;
    }
    if((newHhVal == "00" || newHhVal == "0") && (newMmVal == "00" || newMmVal == "0")) { // If all zeroes => remove time
      hh.val("");
      mm.val("");
    } else {
      if(hhVal != newHhVal) hh.val(newHhVal);
      if(mmVal != newMmVal) mm.val(newMmVal);
    }
  }
}

function extractHoursFromDate(datetime) {
  var a = new String(datetime);
  var b = a.split(" ");
  if (b.length > 1) {
    var c = b[1].split(":");
    if (c != null) {
      return c[0];
    }
  }
  return "";
}

function extractMinutesFromDate(datetime) {
  var a = new String(datetime);
  var b = a.split(" ");
  if (b.length > 1) {
    var c = b[1].split(":");
    if (c.length > 0) {
      var min = c[1];
      if (min != null) {
        return min;
      }
      // Hour has been specified, but no minutes.
      // Return "00" to properly display time.
      return "00";
    }
  }
  return "";
}

function saveDateAndTimeFields() {
  var dateFields = $(".date");
  for(var i = 0, len = dateFields.length; i < len; i++) {
    var dateFieldName = dateFields[i].name;
    if (!dateFieldName) return;

    var fieldName = dateFieldName.replace(/\./g, '\\.');

    var hours = $("#" + fieldName + "-hours")[0];
    var minutes = $("#" + fieldName + "-minutes")[0];
    var date = $("#" + fieldName + "-date")[0];

    var savedVal = "";
    
    if (date && date.value.toString().length) {
      savedVal = date.value;
      if (hours && hours.value.toString().length) {
        savedVal += " " + hours.value;
        if (minutes.value && minutes.value.toString().length) {
         savedVal += ":" + minutes.value;
        }
      }
    }
    
    dateFields[i].value = savedVal;
    
  }
}

/* ^ Datepicker for new documenttypes */