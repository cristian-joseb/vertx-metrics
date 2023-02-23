package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server extends AbstractVerticle {
  private static final Pattern TRAILING_SLASH_PATTERN = Pattern.compile("\\/$");
  private static final Pattern LEADING_SLASH_PATTERN = Pattern.compile("^\\/");

  public static void main(String[] args) {
    var serverMetrics = new ServerMetrics();

    var vertxOptions = new VertxOptions()
      .setMetricsOptions(serverMetrics.initializeMetricsOptions());

    Vertx vertx = Vertx.vertx(vertxOptions);

    Server server = new Server();
    vertx.deployVerticle(new ReadStatusVerticle(), handler -> {
      vertx.deployVerticle(server, handler2 -> {
        System.out.println("Running....");
      });
    });
  }

  private static String getAddressFromRc(RoutingContext rc) {

    String address = rc.normalisedPath();

    Matcher matcher = TRAILING_SLASH_PATTERN.matcher(address);

    if (matcher.find()) {
      address = matcher.replaceFirst("");
    }

    matcher = LEADING_SLASH_PATTERN.matcher(address);

    if (matcher.find()) {
      address = matcher.replaceFirst("");
    }

    return address;
  }

  @Override
  public void start() {
    String route = ".*";
    Router router = Router.router(vertx);
    router.routeWithRegex(route).handler(BodyHandler.create(false));
    router.routeWithRegex(route).handler(this::handle);

    var httpServer = vertx.createHttpServer();
    httpServer
      .requestHandler(router::accept)
      .listen(8080);
  }

  private void handle(RoutingContext routingContext) {

    String address = getAddressFromRc(routingContext);
    vertx.eventBus().request(address, new JsonObject(), handler -> {
      HttpServerResponse serverResponse = routingContext.response();
      if (handler.succeeded()) {
        serverResponse.headers().setAll(handler.result().headers());
        serverResponse.end(handler.result().body().toString());
      } else {
        ReplyException exception = (ReplyException) handler.cause();
        serverResponse.setStatusCode(exception.failureCode());
      }
    });
  }
}
