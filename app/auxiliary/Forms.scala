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
case class BootstrapForm(ip: String, port: Int, key: String)
object BootstrapForm {
   val form: Form[BootstrapForm] = Form(
      mapping(
         "ip" -> text,
         "port" -> number,
         "key" -> text
      )(BootstrapForm.apply)(BootstrapForm.unapply)
   )
}