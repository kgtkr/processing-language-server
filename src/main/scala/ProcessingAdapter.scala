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
    problemsCallback: (JList[Problem]) => Unit
) {
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
    probs => {
      problemsCallback(probs)
    },
    preprocService
  )
  val suggestionGenerator =
    CompletionGenerator(javaMode)

  def uriToPath(uri: String): File = {
    val uriPrefix = "file://"
    File(uri.substring(uriPrefix.length))
  }

  def pathToUri(path: File): String = {
    val uriPrefix = "file://"
    uriPrefix + path.toString
  }
}
