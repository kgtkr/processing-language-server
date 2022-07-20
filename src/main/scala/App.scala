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
  def protocolStdio(): (InputStream, OutputStream) = {
      val input = System.in
      val output = System.out
      System.setOut(System.err)
      (input, output)
  }

  def protocolTcp(port: Int): (InputStream, OutputStream) = {
      val serverSocket = ServerSocket(port.toInt)
      System.out.println("Ready")
      val socket = serverSocket.accept()
      (socket.getInputStream, socket.getOutputStream)
  }

  @main def main(args: String*): Unit = {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    val (input, output) = args match {
      case Seq("stdio") => {
        protocolStdio()
      }
      case Seq("tcp", port) => {
        protocolTcp(port.toInt)
      }
      case Seq(port) => {
        protocolTcp(port.toInt)
      }
      case _ => {
        System.err.println("Invalid arguments")
        System.exit(1)
      }
    }

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
