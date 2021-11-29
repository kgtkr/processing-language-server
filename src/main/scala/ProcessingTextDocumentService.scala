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
import java.net.URI

class ProcessingTextDocumentService(val pls: ProcessingLanguageServer)
    extends TextDocumentService
    with LazyLogging {
  override def didChange(
      params: DidChangeTextDocumentParams
  ): Unit = {
    logger.info("didChange")
    val uri = URI(params.getTextDocument.getUri)
    pls.getAdapter(uri).foreach { adapter =>
      val change = params.getContentChanges.get(0)
      adapter.onChange(uri, change.getText)
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
    val uri = URI(params.getTextDocument.getUri)
    pls.getAdapter(uri).foreach { adapter =>
      adapter.onChange(uri, params.getTextDocument.getText)
    }
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
    val uri = URI(params.getTextDocument.getUri)
    pls
      .getAdapter(uri)
      .map[CompletableFuture[LspEither[JList[CompletionItem], CompletionList]]](
        adapter =>
          adapter
            .generateCompletion(
              uri,
              params.getPosition.getLine,
              params.getPosition.getCharacter
            )
            .thenApply(LspEither.forLeft)
      )
      .getOrElse(
        CompletableFutures
          .computeAsync[LspEither[JList[CompletionItem], CompletionList]](_ =>
            LspEither.forLeft(Collections.emptyList())
          )
      )
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
    val uri = URI(params.getTextDocument.getUri)
    pls
      .getAdapter(uri)
      .map(adapter =>
        CompletableFutures.computeAsync[JList[? <: TextEdit]](_ => {
          adapter
            .format(uri)
            .map(JList.of)
            .getOrElse(JList.of())
        })
      )
      .getOrElse(
        CompletableFuture.completedFuture[JList[? <: TextEdit]](JList.of())
      )
  }
}
