package auxiliary

import play.api.data.Form
import play.api.data.Forms._

// New Network
case class NewNetworkForm(name: String, display: String)
object NewNetworkForm {
   val form: Form[NewNetworkForm] = Form(
      mapping(
         "name" -> text,
         "display" -> text
      )(NewNetworkForm.apply)(NewNetworkForm.unapply)
   )
}

// Bootstrap Form
case class BootstrapKeyForm(key: String)
object BootstrapKeyForm {
   val form: Form[BootstrapKeyForm] = Form(
      mapping(
         "key" -> text
      )(BootstrapKeyForm.apply)(BootstrapKeyForm.unapply)
   )
}

// Post Form
case class PostForm(content: String, response: String, thread: Boolean)
object PostForm {
   val form: Form[PostForm] = Form(
      mapping(
         "content" -> text,
         "response" -> text,
         "thread" -> boolean
      )(PostForm.apply)(PostForm.unapply)
   )
}

// Posts Retrieval AJAX
case class PostRetrievalRequest(epoch: Int, fromTime: Long)
object PostRetrievalRequest {
   //validation 1 goes here
   val userForm = Form(
      mapping(
         "epoch" -> number(0, Int.MaxValue),
         "fromTime" -> longNumber(0, Long.MaxValue),

      )(PostRetrievalRequest.apply)(PostRetrievalRequest.unapply)
   )
}


// Comments Retrieval AJAX
case class CommentsRetrievalRequest(pid: String)
object CommentsRetrievalRequest {
   //validation 1 goes here
   val userForm = Form(
      mapping(
         "pid" -> nonEmptyText
      )(CommentsRetrievalRequest.apply)(CommentsRetrievalRequest.unapply)
   )
}