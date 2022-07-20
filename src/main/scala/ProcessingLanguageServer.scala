package net.kgtkr.processingLanguageServer

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
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import org.eclipse.lsp4j.jsonrpc.messages.Either as LspEither;
import java.util.List as JList
import processing.app.{Base, Platform, Console, Language, Preferences};
import processing.app.contrib.ModeContribution
import processing.mode.java.JavaMode;
import java.io.File
import processing.app.Sketch
import processing.mode.java.JavaBuild
import processing.mode.java.CompletionGenerator
import processing.mode.java.PreprocService2
import org.eclipse.lsp4j.WorkspaceFoldersOptions
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageClient
import processing.mode.java.ErrorChecker2
import processing.app.Problem
import collection.JavaConverters._
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.DiagnosticSeverity
import scala.collection.mutable.Map as MMap
import java.net.URI

class ProcessingLanguageServer
    extends LanguageServer
    with LazyLogging
    with LanguageClientAware {
  val adapters: MMap[File, ProcessingAdapter] = MMap();
  var client: LanguageClient = null;
  val textDocumentService = ProcessingTextDocumentService(this)
  val workspaceService = ProcessingWorkspaceService(this)
  override def exit(): Unit = {
    logger.info("exit")
  }
  override def getTextDocumentService(): TextDocumentService =
    textDocumentService
  override def getWorkspaceService(): WorkspaceService = workspaceService

  def getAdapter(uri: URI): Option[ProcessingAdapter] = {
    for {
      file <- ProcessingAdapter
        .uriToPath(uri)
      if {
        val ext = file.lowerExtension.getOrElse("")
        ext == "pde" || ext == "java"
      }
      rootDir = file.getParentFile
    } yield adapters.getOrElseUpdate(
      rootDir, {
        ProcessingAdapter(rootDir, client)
      }
    )
  }
  override def initialize(
      params: InitializeParams
  ): CompletableFuture[InitializeResult] = {
    ProcessingAdapter.init();
    logger.info("initialize")
    val capabilities = new ServerCapabilities();
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

    val completionOptions = new CompletionOptions();
    completionOptions.setResolveProvider(true);
    completionOptions.setTriggerCharacters(
      List(
        "."
      ).asJava
    );
    capabilities.setCompletionProvider(completionOptions);

    capabilities.setDocumentFormattingProvider(true);

    val result = new InitializeResult(capabilities);
    CompletableFuture.completedFuture(result);
  }
  override def shutdown(): CompletableFuture[Object] = {
    logger.info("shutdown")
    CompletableFuture.completedFuture(null)
  }

  override def connect(client: LanguageClient): Unit = {
    this.client = client
  }
}
