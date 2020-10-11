package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, String> services = new HashMap<>();
  private DBConnector connector;
  private WebClient client;
  private BackgroundPoller poller;

  @Override
  public void start(Future<Void> startFuture) {
    client = WebClient.create(vertx);
    poller = new BackgroundPoller(client);
    connector = new DBConnector(vertx);
    //Future<Void> insertInitialServices = loadInitialDBValues(services);
    connector.populateServices(services)
      .compose(v -> setupHttpServer())
      .setHandler(ar -> {
        System.out.println(services);
        System.out.println("Initial setup hello");
        if (ar.succeeded()) {
          System.out.println("Initial setup ok");
          startFuture.complete();
        } else {
          System.out.println("Initial setup failed");
          startFuture.fail(ar.cause());
        }
      });
  }

  private Future<Void> setupHttpServer() {
    Future<Void> future = Future.future();
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    vertx.setPeriodic(1000 * 5, timerId -> poller.pollServices(services, vertx));
    setRoutes(router);
    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(8080, result -> {
        if (result.succeeded()) {
          System.out.println("KRY code test service started");
          future.complete();
        } else {
          future.fail(result.cause());
        }
      });
    return future;
  }

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());

    router.get("/service").handler(req -> {
      System.out.println("fetching all services...");
      List<JsonObject> jsonServices = services
          .entrySet()
          .stream()
          .map(service ->
              new JsonObject()
                  .put("url", service.getKey())
                  .put("status", service.getValue()))
          .collect(Collectors.toList());
      req.response()
          .putHeader("content-type", "application/json")
          .end(new JsonArray(jsonServices).encode());
    });

    router.post("/service").handler(req -> {
      System.out.println("adding service...");
      JsonObject jsonBody = req.getBodyAsJson();
      connector.insertService(jsonBody.getString("url"), jsonBody.getString("serviceName"))
        .setHandler( ar -> {
          if(ar.succeeded()){
            services.put(jsonBody.getString("url"), "UNKNOWN");
            req.response()
              .putHeader("content-type", "text/plain")
              .setStatusCode(201)
              .end("OK");
          } else {
            req.response()
              .putHeader("content-type", "text/plain")
              .setStatusCode(409)
              .end("FAILED: service name is already taken.");
          }
        });
    });

    router.delete("/service").handler(req -> {
      System.out.println("deleting service...");
      JsonObject jsonBody = req.getBodyAsJson();
      connector.deleteService(jsonBody.getString("url"))
        .setHandler( ar -> {
          if(ar.succeeded()){
            services.remove(jsonBody.getString("url"));
            req.response()
              .putHeader("content-type", "text/plain")
              .setStatusCode(204)
              .end("OK: Service deleted");
          } else {
            req.response()
              .putHeader("content-type", "text/plain")
              .setStatusCode(409)
              .end("Failed to delete service ");
          }
        });
    });
  }
}



