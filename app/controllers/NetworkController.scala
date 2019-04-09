package controllers

import java.net.{InetAddress, UnknownHostException}
import java.security.MessageDigest
import java.util.Base64
import java.util.stream.Collectors

import javax.inject._
import play.api.data.Form
import play.api.mvc._
import auxiliary._
import io.hbt.bubblegum.core.databasing.Post
import org.apache.commons.text.StringEscapeUtils
import play.api.libs.json._

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
         Ok(views.html.networks.show(networkDescription, size, myInformation, BootstrapKeyForm.form))
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
                     print("Successful bootstrap - new network: " + node.getNetworkIdentifier)
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
         Redirect(routes.NetworkController.show(id)).flashing("error" -> "Failed to publish post")
      }

      val successFunction = { data: PostForm =>
         val networkDescription = State.getNetworkDescription(id)
         if(networkDescription != null) {
            val node = State.bubblegum.getNode(networkDescription.getID)
            if(node != null) {
               val safe = data.content.replace("<", "&lt;").replace(">", "&gt;")
               val post = node.savePost(safe)
               if(post == null) {
                  Redirect(routes.NetworkController.show(id)).flashing("error" -> "Failed to publish post")
               }
               else {
                  Redirect(routes.NetworkController.show(id)).flashing("success" -> "Post published successfully!")
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
            BadRequest("{'message':'Invalid request'}")
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
      Ok("hi")
   }

}
