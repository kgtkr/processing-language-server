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
import processing.mode.java.PreprocSketch
import processing.mode.java.JavaTextArea
import java.util.Collections
import processing.mode.java.CompletionCandidate
import javax.swing.DefaultListModel
import org.eclipse.lsp4j.InsertTextFormat
import org.eclipse.lsp4j.CompletionItemKind
import org.jsoup.Jsoup
import java.net.URI

class ProcessingAdapter(
    rootPath: String,
    client: LanguageClient
) extends LazyLogging {
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
  var cps: CompletableFuture[PreprocSketch] =
    CompletableFutures.computeAsync(_ => ???)
  val suggestionGenerator =
    CompletionGenerator(javaMode)
  notifySketchChanged();

  def notifySketchChanged() = {
    val cps = CompletableFuture[PreprocSketch]
    this.cps = cps;
    preprocService.notifySketchChanged()
    errorChecker.notifySketchChanged()
    preprocService.whenDone(ps => {
      cps.complete(ps)
    })
  }

  def uriToPath(uri: String): File = {
    File(new URI(uri))
  }

  def pathToUri(path: File): String = {
    path.toURI.toString
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

  def generateCompletion(
      uri: String,
      line: Int,
      col: Int
  ): CompletableFuture[JList[CompletionItem]] = {
    cps.thenApply(ps => {
      val path = uriToPath(uri)
      val codeIndex =
        sketch.getCode
          .indexWhere(_.getFile == path)
      val code = sketch.getCode(codeIndex)
      val lineStartOffset = code.getProgram
        .split("\n")
        .take(line + 1)
        .mkString("\n")
        .length
      val lineNumber = ps.tabOffsetToJavaLine(codeIndex, lineStartOffset);

      val text = code.getProgram
        .split("\n")(line)
        .substring(0, col)
      val phrase = {
        val method = classOf[JavaTextArea]
          .getDeclaredMethod("parsePhrase", classOf[String])

        method.setAccessible(true)
        method
      }
        .invoke(null, text)
        .asInstanceOf[String]
      logger.debug(s"phrase: $phrase")
      if (phrase != null) {
        logger.debug(s"lineNumber: $lineNumber")
        val candidates = suggestionGenerator
          .preparePredictions(ps, phrase, lineNumber);
        logger.debug("candidates:" + candidates)
        if (candidates != null && !candidates.isEmpty()) {
          Collections.sort(candidates);
          val defListModel = {
            val method = classOf[CompletionGenerator]
              .getDeclaredMethod(
                "filterPredictions",
                classOf[JList[CompletionCandidate]]
              )
            method.setAccessible(true)
            method
              .invoke(null, candidates)
              .asInstanceOf[DefaultListModel[CompletionCandidate]]
          }

          val filtered = Collections.list(defListModel.elements)
          logger.debug("filtered:" + filtered)

          filtered.asScala
            .map(c => {
              val item = new CompletionItem()
              item.setLabel(c.getElementName)
              item.setInsertTextFormat(InsertTextFormat.Snippet)
              item.setInsertText({
                val insert = c.getCompletionString;
                if (insert.contains("( )")) {
                  insert.replace("( )", "($1)")
                } else if (insert.contains(",")) {
                  var n = 1
                  insert
                    .replace("(,", "($1,")
                    .flatMap(c =>
                      c match {
                        case ',' =>
                          n += 1
                          ",$" + n
                        case _ =>
                          c.toString
                      }
                    )
                } else {
                  insert
                }
              })
              item.setKind(c.getType match {
                case 0 => // PREDEF_CLASS
                  CompletionItemKind.Class
                case 1 => // PREDEF_FIELD
                  CompletionItemKind.Constant
                case 2 => // PREDEF_METHOD
                  CompletionItemKind.Function
                case 3 => // LOCAL_CLASS
                  CompletionItemKind.Class
                case 4 => // LOCAL_METHOD
                  CompletionItemKind.Method
                case 5 => // LOCAL_FIELD
                  CompletionItemKind.Field
                case 6 => // LOCAL_VARIABLE
                  CompletionItemKind.Variable
              })
              item.setDetail(Jsoup.parse(c.getLabel).text())
              item
            })
            .asJava

        } else {
          JList.of()
        }
      } else {
        JList.of()
      }
    })
  }
}
