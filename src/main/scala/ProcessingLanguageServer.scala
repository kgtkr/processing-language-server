import org.eclipse.lsp4j.services.{
  LanguageServer,
  TextDocumentService,
  WorkspaceService
}
import org.eclipse.lsp4j.{InitializeResult, InitializeParams}
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.CompletionOptions
import com.typesafe.scalalogging.LazyLogging

class ProcessingLanguageServer extends LanguageServer with LazyLogging {
  def exit(): Unit = {
    logger.info("exit")
  }
  def getTextDocumentService(): TextDocumentService = {
    logger.info("getTextDocumentService")
    ProcessingTextDocumentService()
  }
  def getWorkspaceService(): WorkspaceService = {
    logger.info("getWorkspaceService")
    ProcessingWorkspaceService()
  }
  def initialize(
      params: InitializeParams
  ): CompletableFuture[InitializeResult] = {
    logger.info("initialize")
    val capabilities = new ServerCapabilities();
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

    val completionOptions = new CompletionOptions();
    completionOptions.setResolveProvider(true);
    capabilities.setCompletionProvider(completionOptions);

    val result = new InitializeResult(capabilities);
    return CompletableFuture.completedFuture(result);
  }
  def shutdown(): CompletableFuture[Object] = {
    logger.info("shutdown")
    ???
  }
}

class ProcessingWorkspaceService extends WorkspaceService with LazyLogging {
  def didChangeConfiguration(
      x$0: org.eclipse.lsp4j.DidChangeConfigurationParams
  ): Unit = {
    logger.info("didChangeConfiguration")
  }
  def didChangeWatchedFiles(
      x$0: org.eclipse.lsp4j.DidChangeWatchedFilesParams
  ): Unit = {
    logger.info("didChangeWatchedFiles")
  }
}

class ProcessingTextDocumentService
    extends TextDocumentService
    with LazyLogging {
  def didChange(x$0: org.eclipse.lsp4j.DidChangeTextDocumentParams): Unit = {
    logger.info("didChange")
  }
  def didClose(x$0: org.eclipse.lsp4j.DidCloseTextDocumentParams): Unit = {
    logger.info("didClose")
  }
  def didOpen(x$0: org.eclipse.lsp4j.DidOpenTextDocumentParams): Unit = {
    logger.info("didOpen")
  }
  def didSave(x$0: org.eclipse.lsp4j.DidSaveTextDocumentParams): Unit = {
    logger.info("didSave")
  }
}
