package controllers

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

}
