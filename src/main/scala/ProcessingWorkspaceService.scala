package net.kgtkr.processingLanguageServer

import org.eclipse.lsp4j.services.WorkspaceService
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.lsp4j.DidChangeConfigurationParams
import collection.JavaConverters._
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.FileChangeType

class ProcessingWorkspaceService(val server: ProcessingLanguageServer)
    extends WorkspaceService
    with LazyLogging {
  override def didChangeConfiguration(
      params: DidChangeConfigurationParams
  ): Unit = {}
  override def didChangeWatchedFiles(
      params: DidChangeWatchedFilesParams
  ): Unit = {
    for (change <- params.getChanges.asScala) {
      change.getType match {
        case FileChangeType.Created =>
          server.adapter.sketch.addFile(server.adapter.uriToPath(change.getUri))
          server.adapter.preprocService.notifySketchChanged()
        case FileChangeType.Changed =>
          server.adapter.sketch.getCode
            .find(
              _.getFile == server.adapter.uriToPath(change.getUri)
            )
            .get
            .load()
          server.adapter.preprocService.notifySketchChanged()
        case FileChangeType.Deleted =>
          server.adapter.sketch.removeCode(
            server.adapter.sketch.getCode
              .find(
                _.getFile == server.adapter.uriToPath(change.getUri)
              )
              .get
          )
          server.adapter.preprocService.notifySketchChanged()
      }
    }
  }
}
