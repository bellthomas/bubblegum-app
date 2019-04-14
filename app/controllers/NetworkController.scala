package controllers

import java.io.InputStream
import java.net.{InetAddress, Socket, UnknownHostException}
import java.util.Base64

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import auxiliary._
import io.hbt.bubblegum.core.auxiliary.{ObjectResolutionDetails, Pair}
import javax.inject._
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.collection.mutable

@Singleton
class NetworkController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

   def show(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val networkDescription = State.getNetworkDescription(id)
      if(networkDescription == null) {
         Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
      }
      else {
         val node = State.bubblegum.getNode(networkDescription.getID)
         val size = node.getRoutingTable.getSize
         val myInformationString = node.getServer.getLocal.getHostAddress + "," + node.getServer.getPort + "," + node.getRecipientID
         val myInformation = Base64.getEncoder().encodeToString(myInformationString.getBytes)
         Ok(views.html.networks.show(networkDescription, size, myInformation, BootstrapKeyForm.form, PostForm.form))
      }
   }

   def bootstrapSubmit(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = { formWithErrors: Form[BootstrapKeyForm] =>
         val networkDescription = State.getNetworkDescription(id)
         if(networkDescription == null) {
            Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
         }
         else {
            Redirect(routes.NetworkController.show(id)).flashing("error" -> "Bootstrap failed")

         }
      }

      val successFunction = { data: BootstrapKeyForm =>
         val networkDescription = State.getNetworkDescription(id)
         if(networkDescription == null) {
            Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
         }
         else {
            try {
               val payload : String = new String(Base64.getDecoder.decode(data.key.getBytes))
               val parts = payload.split(",")
               if(parts.length == 3) {
                  val ip = InetAddress.getByName(parts(0))
                  val port = Integer.valueOf(parts(1));
                  val key = parts(2);
                  val node = State.bubblegum.getNode(networkDescription.getID)
                  if (node.bootstrap(ip, port, key)) {
                     Redirect(routes.NetworkController.show(id)).flashing("info" -> "Network Bootstrapped!")
                  }
                  else {
                     Redirect(routes.NetworkController.show(id)).flashing("error" -> "Bootstrap Failed")
                  }
               }
               else {
                  Redirect(routes.NetworkController.show(id)).flashing("error" -> "Invalid Key")
               }
            }
            catch {
               case uhe: UnknownHostException => {
                  Redirect(routes.NetworkController.show(id)).flashing("error" -> "Invalid Key")
               }
               case nfe: NumberFormatException => {
                  Redirect(routes.NetworkController.show(id)).flashing("error" -> "Invalid Key")
               }
               case e: Exception => {
//                  BadRequest(show(id)).flashing("error" -> "An Unknown Error Occurred:" + e.getMessage)
                  Redirect(routes.InstanceController.index()).flashing("error" -> ("Unknown Error Occurred: " + e.getMessage))
               }
            }

         }
      }

      val formValidationResult = BootstrapKeyForm.form.bindFromRequest
      formValidationResult.fold(errorFunction, successFunction)
   }


   def newPost(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = { formWithErrors: Form[PostForm] =>
         Redirect(routes.NetworkController.show(id)).flashing("error" -> "Failed to publish post (form errors)")
      }

      val successFunction = { data: PostForm =>
         val networkDescription = State.getNetworkDescription(id)
         if(networkDescription != null) {
            val node = State.bubblegum.getNode(networkDescription.getID)
            if(node != null) {
               val safe = data.content.replace("<", "&lt;").replace(">", "&gt;")
               val post = if(data.response.length > 0) node.saveResponse(safe, data.response) else node.savePost(safe)
               val redirect = if(data.thread && data.response.length > 0) {
                  routes.NetworkController.showThread(id, new String(Base64.getEncoder.encode(data.response.getBytes)))
               } else {
                  routes.NetworkController.show(id)
               }

               if(post == null) {
                  Redirect(redirect).flashing("error" -> "Failed to publish post (post == null)")
               }
               else {
                  Redirect(redirect).flashing("success" -> "Post published successfully!")
               }
            }
            else {
               Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
            }
         }
         else {
            Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
         }
      }

      val formValidationResult = PostForm.form.bindFromRequest
      formValidationResult.fold(errorFunction, successFunction)
   }


   def getEpoch(hash : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      PostRetrievalRequest.userForm.bindFromRequest().fold(
         formWithErrors => {
            BadRequest("{'message':'Invalid request', 'description':'"+formWithErrors.errors(0)+"'}")
         },
         user => {
            val node = State.getNodeForHash(hash)
            if(node != null) {
               val posts = State.refreshEpoch(node, user.epoch).asScala
               val entities = mutable.ListBuffer[JsValue]()
               for(post <- posts) {
                  if(post.getTimeCreated > user.fromTime) {
                     var ownerDisplay = State.getMeta(post.getNetwork + ":" + post.getOwner, "username")
                     ownerDisplay = if(ownerDisplay == null) post.getOwner else ownerDisplay

                     entities += Json.obj(
                        "owner" -> ownerDisplay,
                        "ownerHash" -> post.getOwner,
                        "content" -> post.getContent,
                        "id" -> post.getID,
                        "time" -> post.getTimeCreated,
                        "response" -> post.getResponse
                     )
                  }
               }

               Ok(Json.stringify(Json.obj("data" -> Json.toJson(entities))))
            }
            else {
               BadRequest("{'message':'No node found.'}")
            }
         }
      )
   }

   def showThread(hash : String, pid : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val plainPostID : String = new String(Base64.getDecoder.decode(pid.getBytes()))
      val plainPostIDParts = plainPostID.split(":")
      if(plainPostIDParts.length == 2) {
         var post = State.getCachedPost(hash, plainPostID);
         if(post == null) {
            // Try fresh retrieval
            post = State.lookupPost(State.getNodeForHash(hash), plainPostIDParts(0), plainPostIDParts(1));
         }

         if(post != null) {
            val nd = State.getNetworkDescription(hash)
            var ownerName = State.getMeta(post.getNetwork + ":" + post.getOwner, "username")
            if(ownerName == null) ownerName = post.getOwner
            if(nd != null) {
               Ok(views.html.networks.thread(post, ownerName, State.getNetworkDescription(hash), PostForm.form))
            } else {
               Redirect(routes.NetworkController.show(hash)).flashing("error" -> "Couldn't find that post")
            }
         } else {
            Redirect(routes.NetworkController.show(hash)).flashing("error" -> "Couldn't find that post")
         }
      } else {
         Redirect(routes.NetworkController.show(hash)).flashing("error" -> "Couldn't find that post")
      }
   }

   def getComments(hash : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      CommentsRetrievalRequest.userForm.bindFromRequest().fold(
         formWithErrors => {
            BadRequest("{'message':'Invalid request'}")
         },
         form => {
            val node = State.getNodeForHash(hash)
            if(node != null) {
               val posts = State.getComments(node, form.pid).asScala
               val entities = mutable.ListBuffer[JsValue]()
               for(post <- posts) {
                  var ownerDisplay = State.getMeta(post.getNetwork + ":" + post.getOwner, "username")
                  ownerDisplay = if(ownerDisplay == null) post.getOwner else ownerDisplay

                  entities += Json.obj(
                     "owner" -> ownerDisplay,
                     "ownerHash" -> post.getOwner,
                     "content" -> post.getContent,
                     "id" -> post.getID,
                     "time" -> post.getTimeCreated,
                     "response" -> post.getResponse
                  )
               }

               Ok(Json.stringify(Json.obj("data" -> Json.toJson(entities))))
            }
            else {
               BadRequest("{'message':'No node found.'}")
            }
         }
      )
   }

   def resource(id : String, hash : String, uri : String) = Action { implicit request: MessagesRequest[AnyContent] =>
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
                     .withHeaders(
                        CACHE_CONTROL -> "max-age=3600", // 1 hour
                        ETAG -> "bubblegum-resource"
                     )
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
