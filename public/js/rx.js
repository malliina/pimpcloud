var webSocket;

var tableContent;

var onconnect = function (payload) {
    setStatus("Connected.");
    webSocket.send(JSON.stringify({cmd: "subscribe"}));
};
var onmessage = function (payload) {
    var event = jQuery.parseJSON(payload.data);
    if (event.event == "ping") {

    } else {
        prepend(event);
    }
};
var onclose = function (payload) {
    setStatus("Closed.");
//    alert('the connection has been closed')
};
var onerror = function (payload) {
    setStatus("Error.");
};
// case class LogEvent(timeStamp: Long, timeFormatted: String, message: String, loggerName: String, threadName: String, level: Level)
var prepend = function (e) {
    var trc;
    var level = e.level;
    if (level == "ERROR") {
        trc = "danger";
    } else if (level == "WARN") {
        trc = "";
    } else {
        trc = "";
    }
    tableContent.prepend("<tr class=" + trc + "><td>" + e.timeFormatted + "</td><td>" + e.message + "</td><td>" + e.loggerName + "</td><td>" + e.level + "</td></tr>")
};
var setStatus = function (newStatus) {
    document.getElementById("status").innerHTML = newStatus;
};
$(document).ready(function () {
    tableContent = $("#logTableBody");
});