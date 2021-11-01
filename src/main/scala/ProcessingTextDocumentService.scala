package net.kgtkr.processingLanguageServer

import org.eclipse.lsp4j.services.TextDocumentService
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.lsp4j.CompletionParams
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.jsonrpc.messages.Either as LspEither;
import java.util.List as JList
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.TextEdit
import scala.annotation.meta.param
import collection.JavaConverters._
import java.io.File
import processing.mode.java.AutoFormat
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.util.Collections
import processing.mode.java.CompletionGenerator
import processing.mode.java.JavaTextArea
import java.util.Arrays
import processing.mode.java.CompletionCandidate
import javax.swing.DefaultListModel
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.MarkupKind
import org.jsoup.Jsoup
import org.eclipse.lsp4j.InsertTextFormat

class ProcessingTextDocumentService(val server: ProcessingLanguageServer)
    extends TextDocumentService
    with LazyLogging {
  override def didChange(
      params: DidChangeTextDocumentParams
  ): Unit = {
    logger.info("didChange")
    val change = params.getContentChanges.get(0)
    server.adapter.sketch.getCode
      .find(
        _.getFile == server.adapter.uriToPath(params.getTextDocument.getUri)
      )
      .get
      .setProgram(change.getText)
    server.adapter.preprocService.notifySketchChanged()
    server.adapter.errorChecker.notifySketchChanged()
  }
  override def didClose(
      params: DidCloseTextDocumentParams
  ): Unit = {
    logger.info("didClose")
  }
  override def didOpen(
      params: DidOpenTextDocumentParams
  ): Unit = {
    logger.info("didOpen")
  }
  override def didSave(
      params: DidSaveTextDocumentParams
  ): Unit = {
    logger.info("didSave")
  }

  override def completion(
      params: CompletionParams
  ): CompletableFuture[LspEither[JList[CompletionItem], CompletionList]] = {
    logger.debug("completion")
    val p =
      CompletableFuture[LspEither[JList[CompletionItem], CompletionList]]()
    server.adapter.preprocService.notifySketchChanged()
    server.adapter.preprocService.whenDone(ps => {
      try {
        val path = server.adapter.uriToPath(params.getTextDocument.getUri)
        val codeIndex =
          server.adapter.sketch.getCode
            .indexWhere(_.getFile == path)
        val code = server.adapter.sketch.getCode(codeIndex)
        val lineStartOffset = code.getProgram
          .split("\n")
          .take(params.getPosition.getLine + 1)
          .mkString("\n")
          .length
        val lineNumber = ps.tabOffsetToJavaLine(codeIndex, lineStartOffset);

        val text = code.getProgram
          .split("\n")(params.getPosition.getLine)
          .substring(0, params.getPosition.getCharacter)
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
          val candidates = server.adapter.suggestionGenerator
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
            p.complete(
              LspEither.forLeft(
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
              )
            )

          } else {
            p.complete(LspEither.forLeft(JList.of()))
          }
        } else {
          p.complete(LspEither.forLeft(JList.of()))
        }
      } catch {
        case e: Exception => {
          logger.error(e.toString)
          p.complete(LspEither.forLeft(JList.of()))
        }
      }
    })
    p
  }

  override def resolveCompletionItem(
      params: CompletionItem
  ): CompletableFuture[CompletionItem] = {
    CompletableFutures.computeAsync(_ => {
      params
    })
  }

  override def formatting(
      params: DocumentFormattingParams
  ): CompletableFuture[JList[? <: TextEdit]] = {
    val path = server.adapter.uriToPath(params.getTextDocument.getUri)
    CompletableFutures.computeAsync(checker => {
      val code =
        server.adapter.sketch.getCode.find(_.getFile == path).get.getProgram

      val newCode = AutoFormat().format(code)
      val end = toLineCol(code, code.length)
      JList.of(
        TextEdit(
          Range(
            Position(0, 0),
            Position(end._1, end._2)
          ),
          newCode
        )
      )
    });
  }
}
