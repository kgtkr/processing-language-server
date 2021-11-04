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

class ProcessingTextDocumentService
    extends TextDocumentService
    with LazyLogging {
  var adapter: ProcessingAdapter = null;

  override def didChange(
      params: DidChangeTextDocumentParams
  ): Unit = {
    logger.info("didChange")
    val change = params.getContentChanges.get(0)
    val code = adapter
      .findCodeByUri(params.getTextDocument.getUri)

    code.foreach { code =>
      code.setProgram(change.getText)
      adapter.notifySketchChanged();
    }
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
    adapter
      .generateCompletion(
        params.getTextDocument.getUri,
        params.getPosition.getLine,
        params.getPosition.getCharacter
      )
      .thenApply(LspEither.forLeft)
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
    CompletableFutures.computeAsync(checker => {
      val code =
        adapter.findCodeByUri(params.getTextDocument.getUri).map(_.getProgram)
      code
        .map(code => {
          val newCode = AutoFormat().format(code)
          val end = ProcessingAdapter
            .toLineCol(code, code.length)
          JList.of(
            TextEdit(
              Range(
                Position(0, 0),
                Position(end._1, end._2)
              ),
              newCode
            )
          )
        })
        .getOrElse(JList.of())
    });
  }
}
