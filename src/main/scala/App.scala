import org.eclipse.lsp4j.launch.LSPLauncher
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger
import com.typesafe.scalalogging.LazyLogging

object App extends LazyLogging {
  @main def main: Unit = {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    logger.debug("Starting LSP server")

    val server = ProcessingLanguageServer();
    val launcher =
      LSPLauncher.createServerLauncher(server, System.in, System.out);
    launcher.startListening();
  }
}
