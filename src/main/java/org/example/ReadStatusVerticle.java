package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

public class ReadStatusVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().consumer("ReadStatus.v1", this::handle);
    startPromise.complete();
  }

  private void handle(Message<Object> message) {
    message.reply("Hello World!", new DeliveryOptions().addHeader("http_status_code", "200"));
  }
}
