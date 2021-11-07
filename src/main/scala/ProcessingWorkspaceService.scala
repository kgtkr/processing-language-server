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
      val uri = URI(change.getUri)
      if (adapter.workspaceIncludeUri(uri)) {
        change.getType match {
          case FileChangeType.Created =>
            ProcessingAdapter
              .uriToPath(uri)
              .foreach(path => {
                adapter.sketch.loadNewTab(
                  path.getName,
                  "pde",
                  true
                )
                adapter.notifySketchChanged();
              })

          case FileChangeType.Changed =>
            adapter
              .findCodeByUri(uri)
              .foreach { code =>
                code.load()
                adapter.notifySketchChanged();
              }
          case FileChangeType.Deleted =>
            adapter
              .findCodeByUri(uri)
              .foreach { code =>
                adapter.sketch.removeCode(code)
                adapter.notifySketchChanged();
              }
        }
      }

    }
  }
}
