package net.kgtkr.processingLanguageServer

import org.eclipse.lsp4j.launch.LSPLauncher
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger
import com.typesafe.scalalogging.LazyLogging
import java.io.File
import java.net.ServerSocket

object App extends LazyLogging {
  @main def main(port: Int): Unit = {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    val serverSocket = ServerSocket(port)
    System.out.println("Ready")

    val socket = serverSocket.accept()

    val server = ProcessingLanguageServer();
    val launcher =
      LSPLauncher.createServerLauncher(
        server,
        socket.getInputStream,
        socket.getOutputStream
      );
    val client = launcher.getRemoteProxy();
    server.connect(client);
    launcher.startListening();
  }
}
