package husein.putera.gradiyanto.vertx.guesser.word;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.*;

@SuppressWarnings("java:S106")
public class Main {

  public static final String EB_ADDRESS_RESULT = "result";
  private final Logger logger;
  private final Scanner scanner;
  private final Map<String, String> deployments;
  private final Vertx vertx;
  private final DeploymentOptions workerOptions;
  private boolean isReceveingResult;
  long currentIndex = 0L;
  long timerId;

  public Main() {
    logger = LoggerFactory.getLogger(Main.class);
    scanner = new Scanner(System.in);
    deployments = new HashMap<>();
    vertx = Vertx.vertx();
    workerOptions = new DeploymentOptions()
      .setThreadingModel(ThreadingModel.WORKER);
    isReceveingResult = false;
  }

  public static void main(String[] args) {
    new Main().start();
  }

  public void start() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    deployVerticles();
    registerConsumer();
    String input;
    while (true) {
      input = receiveInput();
      if (input.equalsIgnoreCase("x")) {
        break;
      }
      if (input.length() >= 3 && input.length() <= 8) {
        isReceveingResult = true;
        System.out.println("Hasil: (tekan tombol Enter untuk melanjutkan)");

        List<String> possibilities = generatePossibilities(input)
          .stream()
          .sorted(Comparator.comparingInt(String::length))
          .toList();
        logger.info("Possible Words [" + possibilities.size() + "]: " + possibilities);
        timerId = vertx.setPeriodic(150, id -> {
          if (currentIndex < possibilities.size()) {
            vertx.eventBus().send(IndonesianVerticle.EB_ADDRESS, possibilities.get((int) currentIndex));
            currentIndex++;
          } else {
            vertx.cancelTimer(timerId);
          }
        });

        scanner.nextLine();
        isReceveingResult = false;
      }

    }
    goodbye();
  }

  private Set<String> generatePossibilities(String input) {
    Set<String> possibilities = new HashSet<>();
    for (int i = 4; i < input.length() - 1; i++) {
      generateWordsRecursive("", input, possibilities, i);
    }
    return possibilities;
  }

  private void generateWordsRecursive(String built, String remaining, Set<String> result, int maxLength) {
    if (built.length() >= maxLength) {
      result.add(built);
      return;
    }
    if (remaining.isEmpty()) {
      return;
    }

    for (int i = 0; i < remaining.length(); i++) {
      char currentChar = remaining.charAt(i);
      String newBuilt = built + currentChar;
      String newRemaining = remaining.substring(0, i) + remaining.substring(i + 1);
      generateWordsRecursive(newBuilt, newRemaining, result, maxLength);
    }
  }

  private void registerConsumer() {
    vertx.eventBus().consumer(EB_ADDRESS_RESULT, message -> {
      if (isReceveingResult) {
        System.out.print(message.body() + " ");
      }
    });
  }

  private String receiveInput() {
    System.out.print("Masukkan kombinasi huruf (3-8 huruf, 'x' untuk keluar): ");
    return scanner.nextLine();
  }

  @SuppressWarnings("java:S106")
  private void goodbye() {
    System.out.println("Aplikasi berhenti. Terima kasih.");
    this.stop();
    System.exit(0);
  }

  private void deployVerticles() {
    vertx.deployVerticle(new IndonesianVerticle(), workerOptions)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          deployments.put(ar.result(), IndonesianVerticle.class.getName());
          logger.info("Successfully deploy [" + IndonesianVerticle.class.getName() + "]");
        } else {
          logger.error("Failed to deploy [" + IndonesianVerticle.class.getName() + "]", ar.cause());
        }
      });
  }

  private void stop() {
    deployments.keySet().forEach(deploymentId -> vertx.undeploy(deploymentId, ar -> {
      if (ar.succeeded()) {
        logger.info("Successfully undeploy [" + deployments.get(deploymentId) + "]");
        deployments.remove(deploymentId);
      } else {
        logger.error("Failed to undeploy [" + deployments.get(deploymentId) + "]", ar.cause());
      }
      })
    );
  }
}
