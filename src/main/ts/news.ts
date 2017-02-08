let eventSource = new EventSource("/news/sse");
eventSource.onmessage = (e) => {
    let li = document.createElement("li");
    li.innerText = e.data;
    document.getElementById("news").appendChild(li);
}