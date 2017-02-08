import * as $ from 'jquery';
window['jQuery'] = $;

$(() => {
  var eventSource = new EventSource("/news/sse");
    eventSource.onmessage = function(e) {
      $("#news").append("<li>" + e.data + "</li>")
    }
});