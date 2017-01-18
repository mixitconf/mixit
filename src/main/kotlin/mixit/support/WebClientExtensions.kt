package mixit.support

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient


fun WebClient.getJson(url: String) = exchange(ClientRequest.GET(url).accept(MediaType.APPLICATION_JSON_UTF8).build())

fun WebClient.getHtml(url: String) = exchange(ClientRequest.GET(url).accept(MediaType.TEXT_HTML).build())

fun WebClient.postJson(url: String, body: Any) = exchange(ClientRequest.POST(url).accept(MediaType.APPLICATION_JSON_UTF8).body(BodyInserters.fromObject(body)))