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

class ProcessingTextDocumentService(val server: ProcessingLanguageServer)
    extends TextDocumentService
    with LazyLogging {
  override def didChange(
      params: DidChangeTextDocumentParams
  ): Unit = {
    logger.info("didChange")
    val change = params.getContentChanges.get(0)
    server.state.sketch.getCode
      .find(
        _.getFile == server.state.uriToPath(params.getTextDocument.getUri)
      )
      .get
      .setProgram(change.getText)
    server.state.preprocService.notifySketchChanged()
    server.state.errorChecker.notifySketchChanged()
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
      position: CompletionParams
  ): CompletableFuture[LspEither[JList[CompletionItem], CompletionList]] = {

    CompletableFutures.computeAsync(checker => {
      LspEither.forLeft(JList.of())
    });
  }

  override def formatting(
      params: DocumentFormattingParams
  ): CompletableFuture[JList[? <: TextEdit]] = {
    val path = server.state.uriToPath(params.getTextDocument.getUri)
    CompletableFutures.computeAsync(checker => {
      val code =
        server.state.sketch.getCode.find(_.getFile == path).get.getProgram

      val newCode = AutoFormat().format(code)
      println((code, newCode))
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
