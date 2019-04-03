package controllers

import java.net.{InetAddress, UnknownHostException}

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
         val myInformation = node.getServer.getLocal.getHostAddress + " " + node.getServer.getPort + " " + node.getRecipientID
         Ok(views.html.networks.show(networkDescription, size, myInformation))
      }
   }

   def bootstrapForm(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val networkDescription = State.getNetworkDescription(id)
      if(networkDescription == null) {
         Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
      }
      else {
         val node = State.bubblegum.getNode(networkDescription.getID)
         Ok(views.html.networks.bootstrap(BootstrapForm.form, routes.NetworkController.bootstrapSubmit(id)))
      }
   }

   def bootstrapSubmit(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = { formWithErrors: Form[BootstrapForm] =>
         BadRequest(views.html.networks.bootstrap(formWithErrors, routes.NetworkController.bootstrapForm(id)))
      }

      val successFunction = { data: BootstrapForm =>
         val networkDescription = State.getNetworkDescription(id)
         if(networkDescription == null) {
            Redirect(routes.InstanceController.index()).flashing("error" -> "Network not found")
         }
         else {
            try {
               val ip = InetAddress.getByName(data.ip)
               val port = data.port;
               val key = data.key;
               val node = State.bubblegum.getNode(networkDescription.getID)
               if(node.bootstrap(ip, port, key)) {
                  Redirect(routes.NetworkController.show(id)).flashing("info" -> "Network Bootstrapped!")
               }
               else {
                  BadRequest(views.html.networks.bootstrap(BootstrapForm.form.fill(data), routes.NetworkController.bootstrapForm(id)))
                     .flashing("error" -> "Bootstrap Failed")
               }
            }
            catch {
               case uhe: UnknownHostException => {
                  BadRequest(views.html.networks.bootstrap(BootstrapForm.form.fill(data), routes.NetworkController.bootstrapForm(id)))
                     .flashing("error" -> "Invalid IP Address")
               }
               case e: Exception => {
                  Redirect(routes.InstanceController.index()).flashing("error" -> ("Unknown Error Occurred: " + e.getMessage))
               }
            }

         }
      }

      val formValidationResult = BootstrapForm.form.bindFromRequest
      formValidationResult.fold(errorFunction, successFunction)
   }

}
