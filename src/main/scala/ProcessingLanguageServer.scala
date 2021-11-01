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

def toLineCol(s: String, offset: Int): (Int, Int) = {
  val line = s.substring(0, offset).count(_ == '\n')
  val col = offset - s.substring(0, offset).lastIndexOf('\n')
  (line, col)
}

class ProcessingLanguageServer
    extends LanguageServer
    with LazyLogging
    with LanguageClientAware {
  var adapter: ProcessingAdapter = null;
  var client: LanguageClient = null;
  var prevDiagnosticReportUris = Set[String]()
  val textDocumentService = ProcessingTextDocumentService()
  val workspaceService = ProcessingWorkspaceService()
  override def exit(): Unit = {
    logger.info("exit")
  }
  override def getTextDocumentService(): TextDocumentService =
    textDocumentService
  override def getWorkspaceService(): WorkspaceService = workspaceService
  override def initialize(
      params: InitializeParams
  ): CompletableFuture[InitializeResult] = {
    Base.setCommandLine();
    Platform.init();
    Preferences.init();
    this.adapter = ProcessingAdapter(
      params.getRootPath,
      probs => {
        val dias = probs.asScala
          .map(prob => {
            val code = adapter.sketch.getCode(prob.getTabIndex)
            val dia = Diagnostic(
              Range(
                Position(
                  prob.getLineNumber,
                  toLineCol(code.getProgram, prob.getStartOffset)._2 - 1
                ),
                Position(
                  prob.getLineNumber,
                  toLineCol(code.getProgram, prob.getStopOffset)._2 - 1
                )
              ),
              prob.getMessage
            );
            dia.setSeverity(if (prob.isError) {
              DiagnosticSeverity.Error
            } else {
              DiagnosticSeverity.Warning
            });
            (
              adapter.pathToUri(code.getFile),
              dia
            )
          })
          .groupBy(_._1)

        for ((uri, dias) <- dias) {
          val params = PublishDiagnosticsParams()
          params.setUri(uri)
          params.setDiagnostics(dias.map(_._2).toList.asJava)
          client.publishDiagnostics(
            params
          );
        }

        for (uri <- prevDiagnosticReportUris.diff(dias.keySet)) {
          val params = PublishDiagnosticsParams()
          params.setUri(uri)
          params.setDiagnostics(JList.of())
          client.publishDiagnostics(
            params
          );
        }
        prevDiagnosticReportUris = dias.keySet
      }
    )
    this.textDocumentService.adapter = this.adapter
    this.workspaceService.adapter = this.adapter

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
