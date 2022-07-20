package net.kgtkr.processingLanguageServer

import org.eclipse.lsp4j.launch.LSPLauncher
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger
import com.typesafe.scalalogging.LazyLogging
import java.io.File
import java.net.ServerSocket
import java.io.{InputStream, OutputStream}

object App extends LazyLogging {
  @main def mainWithTcp(port: Int): Unit = {
    start({
      val serverSocket = ServerSocket(port)
      System.out.println("Ready")
      val socket = serverSocket.accept()
      (socket.getInputStream, socket.getOutputStream)
    })
  }

  @main def mainWithStdio(): Unit = {
    start({
      val input = System.in
      val output = System.out
      System.setOut(System.err)
      (input, output)
    })
  }

  def start(io: => (InputStream, OutputStream)): Unit = {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    val (input, output) = io

    val server = ProcessingLanguageServer();
    val launcher =
      LSPLauncher.createServerLauncher(
        server,
        input,
        output
      );
    val client = launcher.getRemoteProxy();
    server.connect(client);
    launcher.startListening();
  }
}
