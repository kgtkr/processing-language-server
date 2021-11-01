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

class ProcessingAdapter(
    rootPath: String,
    client: LanguageClient
) {
  Base.setCommandLine();
  Platform.init();
  Preferences.init();

  val javaMode = ModeContribution
    .load(
      null,
      Platform.getContentFile("modes/java"),
      "processing.mode.java.JavaMode"
    )
    .getMode()
    .asInstanceOf[JavaMode]
  val pdeFolder = File(rootPath)
  val pdeFile = File(pdeFolder, pdeFolder.getName() + ".pde");
  val sketch = Sketch(pdeFile.toString, javaMode);
  val completionGenerator = CompletionGenerator(javaMode);
  val preprocService = PreprocService2(javaMode, sketch);
  val errorChecker = ErrorChecker2(
    updateProblems,
    preprocService
  )
  val suggestionGenerator =
    CompletionGenerator(javaMode)
  notifySketchChanged();

  def notifySketchChanged() = {
    preprocService.notifySketchChanged()
    errorChecker.notifySketchChanged()
  }

  def uriToPath(uri: String): File = {
    val uriPrefix = "file://"
    File(uri.substring(uriPrefix.length))
  }

  def pathToUri(path: File): String = {
    val uriPrefix = "file://"
    uriPrefix + path.toString
  }

  var prevDiagnosticReportUris = Set[String]()

  def updateProblems(probs: JList[Problem]): Unit = {
    val dias = probs.asScala
      .map(prob => {
        val code = sketch.getCode(prob.getTabIndex)
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
          pathToUri(code.getFile),
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
}
