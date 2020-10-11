package se.kry.codetest;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;

import java.util.HashMap;
import java.util.List;

public class DBConnector {

  private final String DB_PATH = "poller.db";
  private final SQLClient client;

  public DBConnector(Vertx vertx){
    JsonObject config = new JsonObject()
        .put("url", "jdbc:sqlite:" + DB_PATH)
        .put("driver_class", "org.sqlite.JDBC")
        .put("max_pool_size", 30);

    client = JDBCClient.createShared(vertx, config);
  }

  public Future<ResultSet> query(String query) {
    return query(query, new JsonArray());
  }

  public Future<ResultSet> query(String query, JsonArray params) {
    if(query == null || query.isEmpty()) {
      return Future.failedFuture("Query is null or empty");
    }
    if(!query.endsWith(";")) {
      query = query + ";";
    }

    Future<ResultSet> queryResultFuture = Future.future();

    client.queryWithParams(query, params, result -> {
      if(result.failed()){
        System.out.println(result.cause());
        queryResultFuture.fail(result.cause());
      } else {
        queryResultFuture.complete(result.result());
      }
    });
    return queryResultFuture;
  }

  public Future<Void> loadInitialDBValues(HashMap<String, String> services) {
    Future<Void> future = Future.future();

    Future<Void> i1 = insertService("www.kry.se", "kry");
    Future<Void> i2 = insertService("www.google.com", "google");
    Future<Void> i3 = insertService("www.reddit.com", "reddit");

    CompositeFuture.all(i1, i2, i3).setHandler(ar -> {
      System.out.println(services);
      if (ar.succeeded()) {
        future.complete();
      } else {
        future.fail(ar.cause());
      }
    });
    return future;
  }

  public Future<Void> insertService(String url, String serviceName) {
    Future<Void> future = Future.future();

    System.out.println("Starting insert");
    String queryString = "INSERT INTO service (url, service_name) VALUES( ?, ? )";
    JsonArray params = new JsonArray().add(url).add(serviceName);

    query(queryString, params).setHandler(done -> {
      if(done.succeeded()){
        System.out.println("inserted " + serviceName);
        future.complete();
      } else {
        System.out.println("couldn't insert " + serviceName);
        done.cause().printStackTrace();
        future.fail(done.cause());
      }
    });

    return future;
  }

  public Future<Void> deleteService(String url) {
    Future<Void> future = Future.future();

    System.out.println("Starting delete");
    String queryString = "DELETE FROM service WHERE url = ?;";
    JsonArray params = new JsonArray().add(url);

    query(queryString, params).setHandler(done -> {
      if(done.succeeded()){
        System.out.println("deleted " + url);
        future.complete();
      } else {
        System.out.println("couldn't delete " + url);
        done.cause().printStackTrace();
        future.fail(done.cause());
      }
    });

    return future;
  }

  public Future<Void> populateServices(HashMap<String, String> services) {
    Future<Void> future = Future.future();
    String query_string = "SELECT * FROM service";
    query(query_string).setHandler( res -> {
      if(res.succeeded()){
        System.out.println(res.result().getResults());
        List<JsonObject> servicesAsJson = res.result().getRows();

        for (JsonObject row : servicesAsJson) {
          String url = row.getString("url");
          String serviceName = row.getString("service_name");
          String creationTime = row.getString("creation_time");
          services.put(url, "UNKNOWN");
        }
        future.complete();
      } else {
        System.out.println(res.cause());
        future.fail(res.cause());
      }
    });

    return future;
  }
}
