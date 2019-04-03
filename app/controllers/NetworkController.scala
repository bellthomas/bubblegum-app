package controllers

import javax.inject._
import play.api.mvc._

import collection.JavaConverters._
import play.api.data.Form
import play.api.data.Forms._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class NetworkController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

   /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
   def index = Action { implicit request: MessagesRequest[AnyContent] =>
      val networks = State.bubblegum.getNodeIdentifiers.asScala;
      val names = networks
         .map { State.getNetworkDescription(_) }
         .filter { _ != null }

      Ok(views.html.networks.list(names))
//      Ok(views.html.index("Your new application is ready."))
   }

   def createForm = Action { implicit request: MessagesRequest[AnyContent] =>
      // Pass an unpopulated form to the template
      Ok(views.html.networks.create(BasicForm.form, routes.NetworkController.createSubmit()))
   }

   def createSubmit = Action { implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = { formWithErrors: Form[BasicForm] =>
         // This is the bad case, where the form had validation errors.
         // Let's show the user the form again, with the errors highlighted.
         // Note how we pass the form with errors to the template.
         BadRequest(views.html.networks.create(formWithErrors, routes.NetworkController.createSubmit()))
      }

      val successFunction = { data: BasicForm =>
         // This is the good case, where the form was successfully parsed as a Data object.
//         val widget = Widget(name = data.name, price = data.price)
//         widgets.append(widget)
         val id = State.bubblegum.createNode().getIdentifier
         State.newNetworkdescription(id, data.name)
         Redirect(routes.NetworkController.index()).flashing("info" -> "Widget added!")
      }

      val formValidationResult = BasicForm.form.bindFromRequest
      formValidationResult.fold(errorFunction, successFunction)
   }


   def editForm(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      // Pass an unpopulated form to the template
      val networkDescription = State.getNetworkDescription(id)
      if(networkDescription == null) {
         Redirect(routes.NetworkController.index()).flashing("error" -> "No network with that ID found")
      }
      else {
         val form = BasicForm.form.fill(BasicForm(networkDescription.getName, 1))
         Ok(views.html.networks.create(form, routes.NetworkController.editSubmit(id)))
      }
   }

   def editSubmit(id : String) = Action { implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = { formWithErrors: Form[BasicForm] =>
         // This is the bad case, where the form had validation errors.
         // Let's show the user the form again, with the errors highlighted.
         // Note how we pass the form with errors to the template.
         BadRequest(views.html.networks.create(formWithErrors, routes.NetworkController.editSubmit(id)))
      }

      val successFunction = { data: BasicForm =>
         // This is the good case, where the form was successfully parsed as a Data object.
         //         val widget = Widget(name = data.name, price = data.price)
         //         widgets.append(widget)
         if(State.updateNetworkDescription(id, data.name) != null) {
            Redirect(routes.NetworkController.index()).flashing("info" -> "Widget added!")
         }
         else {
            Redirect(routes.NetworkController.index()).flashing("error" -> "Failed to update network")
         }

      }

      val formValidationResult = BasicForm.form.bindFromRequest
      formValidationResult.fold(errorFunction, successFunction)
   }


   def showNetwork(id : String) = Action {
      val networkDescription = State.getNetworkDescription(id)
      if(networkDescription == null) {
         Redirect(routes.NetworkController.index()).flashing("error" -> "Network not found")
      }
      else {
         Ok(views.html.networks.show(networkDescription))
      }

   }


}

case class BasicForm(name: String, age: Int)
object BasicForm {
   val form: Form[BasicForm] = Form(
      mapping(
         "name" -> text,
         "price" -> number
      )(BasicForm.apply)(BasicForm.unapply)
   )
}