package controllers

import java.io.InputStream
import java.net.Socket

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import io.hbt.bubblegum.core.auxiliary.{Pair, ObjectResolutionDetails}
import javax.inject._
import play.api.mvc._

/**
 * This controller creates an `Action` that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param cc standard controller components
 */
@Singleton
class AsyncController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
  /**
   * Creates an Action that returns a plain text message after a delay
   * of 1 second.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/message`.
   */
  def chunked(id : String, hash : String, uri : String) = Action { implicit request: MessagesRequest[AnyContent] =>
     try {
        val node = State.getNodeForHash(id)
        if(node != null) {
           val details : ObjectResolutionDetails = node.requestResource(hash, uri)
           if(details != null) {
              val data : Pair[Socket, InputStream]  = node.getResourceClient(details)
              if (data != null) {
                 val dataContent: Source[ByteString, _] = StreamConverters.fromInputStream(() => data.getSecond)
                 Ok.chunked(dataContent)
                    .as(details.mimeType)
                 //                 .withHeaders(
                 //                      CACHE_CONTROL -> "max-age=3600",
                 //                      ETAG -> "test"
                 //                 )
              } else {
                 NotFound("")
              }
           } else {
              NotFound("")
           }
        } else {
           NotFound("")
        }
     } catch {
        case _: Exception => NotFound("")
     }
  }


}
