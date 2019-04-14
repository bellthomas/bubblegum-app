package controllers

import javax.inject._
import play.api.mvc._

import collection.JavaConverters._
import play.api.data.Form
import auxiliary._
import io.hbt.bubblegum.core.kademlia.BubblegumNode.Builder

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class InstanceController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

   /**
   * Create an Action to render an HTML page with a header message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
   def index = Action { implicit request: MessagesRequest[AnyContent] =>
      val networks = State.bubblegum.getNodeIdentifiers.asScala;
      val names = networks
         .map { State.idToHash(_) }
         .map { State.getNetworkDescription(_) }
         .filter { _ != null }

      Ok(views.html.networks.list(names))
   }

   def createForm = Action { implicit request: MessagesRequest[AnyContent] =>
      // Pass an unpopulated form to the template
      Ok(views.html.networks.create(NewNetworkForm.form, routes.InstanceController.createSubmit(), "Create"))
   }

   def createSubmit = Action { implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = { formWithErrors: Form[NewNetworkForm] =>
         BadRequest(views.html.networks.create(formWithErrors, routes.InstanceController.createSubmit(), "Create"))
      }

      val successFunction = { data: NewNetworkForm =>
         val node = State.bubblegum.createNode()
         println(node.getNodeIdentifier.toString)
         if(node != null) {
            val hash = State.newNetworkDescription(node.getIdentifier, data.name, data.display, State.randomColour()).getHash
            Redirect(routes.NetworkController.show(hash)).flashing("success" -> "Network created successfully!")
         }
         else {
            Redirect(routes.InstanceController.index()).flashing("error" -> "An internal error occurred")
         }
      }

      val formValidationResult = NewNetworkForm.form.bindFromRequest
      formValidationResult.fold(errorFunction, successFunction)
   }


   def editForm(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val networkDescription = State.getNetworkDescription(id)
      if(networkDescription == null) {
         Redirect(routes.InstanceController.index()).flashing("error" -> "No network with that ID found")
      }
      else {
         val form = NewNetworkForm.form.fill(NewNetworkForm(networkDescription.getName, networkDescription.getDisplayName))
         Ok(views.html.networks.create(form, routes.InstanceController.editSubmit(id), "Edit"))
      }
   }

   def editSubmit(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = { formWithErrors: Form[NewNetworkForm] =>
         BadRequest(views.html.networks.create(formWithErrors, routes.InstanceController.editSubmit(id), "Edit"))
      }

      val successFunction = { data: NewNetworkForm =>
         if(State.updateNetworkDescription(id, data.name, data.display) != null) {
            Redirect(routes.NetworkController.show(id)).flashing("info" -> "Network details amended.")
         }
         else {
            Redirect(routes.InstanceController.index()).flashing("error" -> "Failed to update network.")
         }

      }

      val formValidationResult = NewNetworkForm.form.bindFromRequest
      formValidationResult.fold(errorFunction, successFunction)
   }



}

