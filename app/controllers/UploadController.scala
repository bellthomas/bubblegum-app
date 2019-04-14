package controllers

import java.io.File
import java.nio.file.{Files, Path, Paths}

import javax.inject._
import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Files.TemporaryFile
import play.api.libs.streams._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.core.parsers.Multipart.FileInfo

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._


case class UploadData(name: String)

/**
  * This controller handles a file upload.
  */
@Singleton
class UploadController @Inject() (cc:MessagesControllerComponents)
                               (implicit executionContext: ExecutionContext)
   extends MessagesAbstractController(cc) {


   val form = Form(
      mapping(
         "name" -> text
      )(UploadData.apply)(UploadData.unapply)
   )

   /**
     * Renders a start page.
     */
   def index(hash : String) = Action { implicit request =>
      val description = State.getNetworkDescription(hash)

      if(description == null) {
         Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
      } else {
         val node = State.bubblegum.getNode(description.getID)
         val files = State.getUploadedResources(hash).asScala
         Ok(views.html.upload(hash, description.getName, node.getNodeIdentifier.toString, files, form))
      }
   }

   type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

   /**
     * Uses a custom FilePartHandler to return a type of "File" rather than
     * using Play's TemporaryFile class.  Deletion must happen explicitly on
     * completion, rather than TemporaryFile (which uses finalization to
     * delete temporary files).
     *
     * @return
     */
   private def handleFilePartAsFile: FilePartHandler[File] = {
      case FileInfo(partName, filename, contentType, _) =>
//         val description = State.getNetworkDescription(hash)
//         if (description != null) {
//
//         }
         val path: Path = Files.createTempFile("multipartBody", "tempFile")
         val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(path)
         val accumulator: Accumulator[ByteString, IOResult] = Accumulator(fileSink)
         accumulator.map {
            case IOResult(count, status) =>
               FilePart(partName, filename, contentType, path.toFile)
         }
   }

   /**
     * A generic operation on the temporary file that deletes the temp file after completion.
     */
   private def operateOnTempFile(file: File) = {
      val size = Files.size(file.toPath)
      Files.deleteIfExists(file.toPath)
      size
   }

   /**
     * Uploads a multipart file as a POST request.
     *
     * @return
     */
   def upload(hash : String) = Action(parse.multipartFormData) { implicit request =>
      form.bindFromRequest().fold(
         formWithErrors => {
            BadRequest("Failed 3")
         },
         form => {
            request.body.file("file").map { picture =>
               val network = State.getNetworkDescription(hash)
               if(network != null) {
                  // only get the last part of the filename
                  // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
                  val filename = Paths.get(picture.filename).getFileName

                  try {
                     if(Files.notExists(Paths.get(io.hbt.bubblegum.core.Configuration.RESOLVER_ASSETS_FOLDER, network.getID))) {
                        Files.createDirectory(Paths.get(io.hbt.bubblegum.core.Configuration.RESOLVER_ASSETS_FOLDER, network.getID))
                     }
                     picture.ref.copyTo(Paths.get(
                        io.hbt.bubblegum.core.Configuration.RESOLVER_ASSETS_FOLDER,
                        network.getID,
                        filename.toString
                     ), replace = true)
                     Ok("File uploaded")
                  }
                  catch {
                     case e: Exception => BadRequest("Failed (exception "+e.getMessage+")")
                  }

               } else {
                  BadRequest("Failed 1")
               }
            }.getOrElse {
               BadRequest("Failed 2")
            }
         }
      )

   }


}