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
import processing.app.SketchCode
import scala.util.Try
import org.eclipse.lsp4j.TextEdit
import processing.mode.java.AutoFormat

extension (file: File)
  def lowerExtension: Option[String] = {
    val s = file.toString
    val dot = s.lastIndexOf('.')
    if (dot == -1) None
    else Some(s.substring(dot + 1).toLowerCase)
  }

object ProcessingAdapter {
  def uriToPath(uri: URI): Option[File] = {
    Try(File(uri)).toOption
  }

  def pathToUri(path: File): URI = {
    path.toURI
  }

  def toLineCol(s: String, offset: Int): (Int, Int) = {
    val line = s.substring(0, offset).count(_ == '\n')
    val col = offset - s.substring(0, offset).lastIndexOf('\n')
    (line, col)
  }

  def init() = {
    Base.setCommandLine();
    Platform.init();
    Preferences.init();
  }
}

class ProcessingAdapter(
    rootPath: File,
    client: LanguageClient
) extends LazyLogging {
  val javaMode = ModeContribution
    .load(
      null,
      Platform.getContentFile("modes/java"),
      "processing.mode.java.JavaMode"
    )
    .getMode()
    .asInstanceOf[JavaMode]
  val pdeFolder = rootPath
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

  def findCodeByUri(uri: URI): Option[SketchCode] = {
    for {
      path <- ProcessingAdapter.uriToPath(uri)
      code <- sketch.getCode.find(_.getFile == path)
    } yield code
  }

  var prevDiagnosticReportUris = Set[URI]()

  def updateProblems(probs: JList[Problem]): Unit = {
    val dias = probs.asScala
      .map(prob => {
        val code = sketch.getCode(prob.getTabIndex)
        val dia = Diagnostic(
          Range(
            Position(
              prob.getLineNumber,
              ProcessingAdapter
                .toLineCol(code.getProgram, prob.getStartOffset)
                ._2 - 1
            ),
            Position(
              prob.getLineNumber,
              ProcessingAdapter
                .toLineCol(code.getProgram, prob.getStopOffset)
                ._2 - 1
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
          ProcessingAdapter.pathToUri(code.getFile),
          dia
        )
      })
      .groupBy(_._1)

    for ((uri, dias) <- dias) {
      val params = PublishDiagnosticsParams()
      params.setUri(uri.toString)
      params.setDiagnostics(dias.map(_._2).toList.asJava)
      client.publishDiagnostics(
        params
      );
    }

    for (uri <- prevDiagnosticReportUris.diff(dias.keySet)) {
      val params = PublishDiagnosticsParams()
      params.setUri(uri.toString)
      params.setDiagnostics(JList.of())
      client.publishDiagnostics(
        params
      );
    }
    prevDiagnosticReportUris = dias.keySet
  }

  def convertCompletionCandidate(c: CompletionCandidate): CompletionItem = {
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
  }

  def parsePhrase(text: String): Option[String] = {
    Option({
        val method = classOf[JavaTextArea]
          .getDeclaredMethod("parsePhrase", classOf[String])

        method.setAccessible(true)
        method
      }
        .invoke(null, text)
        .asInstanceOf[String]
    )
  }

  def filterPredictions(
      candidates: JList[CompletionCandidate]
  ): JList[CompletionCandidate] = {
    val filtered = {
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
    Collections.list(filtered.elements)
  }

  def generateCompletion(
      uri: URI,
      line: Int,
      col: Int
  ): CompletableFuture[JList[CompletionItem]] = {
    cps.thenApply(ps => {
      val result =
        for {
          code <- this.findCodeByUri(uri)
          codeIndex = (0 until sketch.getCodeCount()).find(
            i => sketch.getCode(i) == code
          ).get
          lineStartOffset = code.getProgram
            .split("\n")
            .take(line + 1)
            .mkString("\n")
            .length
          lineNumber = ps.tabOffsetToJavaLine(codeIndex, lineStartOffset);

          text = code.getProgram
            .split("\n")(line) // TODO: 範囲外のエラー処理
            .substring(0, col)
          phrase <- parsePhrase(text)
          _ = logger.debug(s"phrase: $phrase")
          _ = logger.debug(s"lineNumber: $lineNumber")
          candidates <- Option(
            suggestionGenerator
              .preparePredictions(ps, phrase, lineNumber)
          ).filter(x => !x.isEmpty())
          _ = Collections.sort(candidates)
          _ = logger.debug("candidates:" + candidates)
          filtered = filterPredictions(candidates)
          _ = logger.debug("filtered:" + filtered)
        } yield filtered.asScala
          .map(convertCompletionCandidate)
          .asJava

      result.getOrElse(JList.of())
    })
  }

  def onChange(uri: URI, text: String): Unit = {
    val code = this
      .findCodeByUri(uri)

    code.foreach { code =>
      code.setProgram(text)
      this.notifySketchChanged();
    }
  }

  def format(uri: URI): Option[TextEdit] = {
    val code =
      this
        .findCodeByUri(uri)
        .map(_.getProgram)
    code
      .map(code => {
        val newCode = AutoFormat().format(code)
        val end = ProcessingAdapter
          .toLineCol(code, code.length)
        TextEdit(
          Range(
            Position(0, 0),
            Position(end._1, end._2)
          ),
          newCode
        )
      })
  }
}
