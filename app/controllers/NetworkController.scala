package controllers

import java.net.{InetAddress, UnknownHostException}
import java.util.Base64

import javax.inject._
import play.api.data.Form
import play.api.mvc._
import auxiliary._

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
         Ok(views.html.networks.show(networkDescription, size, myInformation, BootstrapKeyForm.form, routes.NetworkController.bootstrapSubmit(id)))
      }
   }

   def bootstrapSubmit(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = { formWithErrors: Form[BootstrapKeyForm] =>
         val networkDescription = State.getNetworkDescription(id)
         if(networkDescription == null) {
            Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
         }
         else {
            val node = State.bubblegum.getNode(networkDescription.getID)
            val size = node.getRoutingTable.getSize
            val myInformationString = node.getServer.getLocal.getHostAddress + "," + node.getServer.getPort + "," + node.getRecipientID
            val myInformation = Base64.getEncoder().encodeToString(myInformationString.getBytes)
            BadRequest(views.html.networks.show(networkDescription, size, myInformation, formWithErrors, routes.NetworkController.bootstrapSubmit(id)))
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

}
