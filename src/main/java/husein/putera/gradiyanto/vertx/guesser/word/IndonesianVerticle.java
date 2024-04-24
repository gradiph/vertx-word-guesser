package husein.putera.gradiyanto.vertx.guesser.word;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

public class IndonesianVerticle extends AbstractVerticle {

  public static final String EB_ADDRESS = "indonesian";
  private final Logger logger;

  public IndonesianVerticle() {
    logger = LoggerFactory.getLogger(this.getClass());
  }

  @Override
  public void start() throws Exception {
    vertx.eventBus().consumer(EB_ADDRESS, message -> {
      String word = message.body().toString();
      logger.debug("Received message: " + word);

      isValid(word)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            vertx.eventBus().send(Main.EB_ADDRESS_RESULT, word);
          } else {
            logger.info("Word [" + word + "] is not Indonesian with cause: " + ar.cause());
          }
        });
    });
  }

  private Future<Boolean> isValid(String word) {
    return Future.future(event -> {
      HttpClient httpClient = vertx.createHttpClient();
      httpClient.request(HttpMethod.GET, "kateglo.lostfocus.org", "/api.php?format=json&phrase=" + word)
        .onSuccess(request -> request.send()
          .onSuccess(response -> {
            if (response.statusCode() == 200) {
              response.body()
                .onSuccess(buffer -> {
                  try {
                    String body = buffer.toString().replace("\n", "");
                    JsonObject jsonBody = new JsonObject(body);
                    logger.debug("Result of [" + word + "] : " + jsonBody.toString());
                    event.complete(true);
                  } catch (Exception e) {
                    event.fail(e);
                  }
                })
                .onFailure(event::fail);
            }
          })
          .onFailure(event::fail)
        )
        .onFailure(event::fail);
    });
  }
}
