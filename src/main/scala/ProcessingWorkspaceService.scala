package net.kgtkr.processingLanguageServer

import org.eclipse.lsp4j.services.WorkspaceService
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.lsp4j.DidChangeConfigurationParams
import collection.JavaConverters._
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.FileChangeType
import java.net.URI

class ProcessingWorkspaceService extends WorkspaceService with LazyLogging {
  var adapter: ProcessingAdapter = null;

  override def didChangeConfiguration(
      params: DidChangeConfigurationParams
  ): Unit = {}
  override def didChangeWatchedFiles(
      params: DidChangeWatchedFilesParams
  ): Unit = {
    logger.info("didChangeWatchedFiles: " + params)
    for (change <- params.getChanges.asScala) {
      change.getType match {
        case FileChangeType.Created =>
          adapter.workspace.addDocument(URI(change.getUri), change)
          adapter.notifySketchChanged();
        case FileChangeType.Changed =>
          adapter.workspace.updateDocument(URI(change.getUri), change.ge)
          adapter.notifySketchChanged();
        case FileChangeType.Deleted =>
          adapter.workspace.removeDocument(URI(change.getUri))
          adapter.notifySketchChanged();
      }
    }
  }
}
