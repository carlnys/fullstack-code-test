package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.List;
import java.util.Map;
import java.util.Random;
/*
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .send(ar -> {
    if (ar.succeeded()) {
      // Obtain response
      HttpResponse<Buffer> response = ar.result();

      System.out.println("Received response with status code" + response.statusCode());
    } else {
      System.out.println("Something went wrong " + ar.cause().getMessage());
    }
  })
 */
public class BackgroundPoller {
  WebClient client;
  public BackgroundPoller(WebClient client) {
    this.client = client;
  }

  public void pollServices(Map<String, String> services, Vertx vertx) {
    //System.out.println(services);
    services.forEach((k, v) -> {
      //System.out.println("Calling " + k);
      client
        .get(443, k, "/")
        .ssl(true)
        .send( ar -> {
          if (ar.succeeded()) {
            //System.out.println("Calling " + k);
            HttpResponse<Buffer> response = ar.result();
            int statusCode = response.statusCode();
            //System.out.println(statusCode);
            if(statusCode >= 200 && statusCode <= 299) {
              services.put(k, "OK");
            } else {
              services.put(k, "FAILED");
            }
          } else {
            services.put(k, "FAILED");
            System.out.println(ar.cause());
            //System.out.println("Request from periodic poller failed");
          }
        });
    });
  }
}
