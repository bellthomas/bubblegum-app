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
case class PostForm(content: String)
object PostForm {
   val form: Form[PostForm] = Form(
      mapping(
         "content" -> text
      )(PostForm.apply)(PostForm.unapply)
   )
}