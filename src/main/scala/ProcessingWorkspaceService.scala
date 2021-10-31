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
          server.state.sketch.addFile(server.state.uriToPath(change.getUri))
          server.state.preprocService.notifySketchChanged()
        case FileChangeType.Changed =>
          server.state.sketch.getCode
            .find(
              _.getFile == server.state.uriToPath(change.getUri)
            )
            .get
            .load()
          server.state.preprocService.notifySketchChanged()
        case FileChangeType.Deleted =>
          server.state.sketch.removeCode(
            server.state.sketch.getCode
              .find(
                _.getFile == server.state.uriToPath(change.getUri)
              )
              .get
          )
          server.state.preprocService.notifySketchChanged()
      }
    }
  }
}
