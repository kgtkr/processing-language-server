package net.kgtkr.processingLanguageServer

import org.eclipse.lsp4j.services.WorkspaceService
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.lsp4j.DidChangeConfigurationParams
import collection.JavaConverters._
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.FileChangeType

class ProcessingWorkspaceService extends WorkspaceService with LazyLogging {
  var adapter: ProcessingAdapter = null;

  override def didChangeConfiguration(
      params: DidChangeConfigurationParams
  ): Unit = {}
  override def didChangeWatchedFiles(
      params: DidChangeWatchedFilesParams
  ): Unit = {
    for (change <- params.getChanges.asScala) {
      change.getType match {
        case FileChangeType.Created =>
          adapter.sketch.addFile(adapter.uriToPath(change.getUri))
          adapter.notifySketchChanged();
        case FileChangeType.Changed =>
          adapter.sketch.getCode
            .find(
              _.getFile == adapter.uriToPath(change.getUri)
            )
            .get
            .load()
          adapter.notifySketchChanged();
        case FileChangeType.Deleted =>
          adapter.sketch.removeCode(
            adapter.sketch.getCode
              .find(
                _.getFile == adapter.uriToPath(change.getUri)
              )
              .get
          )
          adapter.notifySketchChanged();
      }
    }
  }
}
