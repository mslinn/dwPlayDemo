package utils

import javax.inject.Inject

import scala.concurrent.Future

import play.api._
import play.api.i18n.{I18nSupport,Messages,MessagesApi}
import play.api.mvc._, Results.Ok
import play.api.http.HttpFilters
import play.filters.csrf.CSRFFilter

import controllers.routes

class TrustXForwardedFilter extends Filter {
  def apply(nextFilter:RequestHeader => Future[Result])(request:RequestHeader):Future[Result] = {
    val newRequest = request.headers.get("X-Forwarded-Proto").collect {
       case "https" => request.copy(secure=true)
    }.getOrElse(request)
    nextFilter(newRequest)
  }
} 

/**
 * This MUST be the last filter in the chain, otherwise we'd bypass CSRF.
 */
class HttpsOnlyFilter @Inject() (val messagesApi:MessagesApi) extends Filter with I18nSupport {
  def apply(nextFilter:RequestHeader => Future[Result])(request:RequestHeader):Future[Result] = {
    implicit val r = request
    request.headers.get("X-Forwarded-Proto").collect {
      case "https" => nextFilter(request)
    }.getOrElse(Future.successful(Ok(views.html.errors.onlyHttsAllowed())))
  }
}

class Filters @Inject() (
    configuration:Configuration, 
    csrfFilter:CSRFFilter, 
    trustXForwardedFilter:TrustXForwardedFilter, 
    httpsOnlyFilter:HttpsOnlyFilter) extends HttpFilters {
  
  val map = Map("application.trustXForwarded" -> trustXForwardedFilter, "application.httpsOnly" -> httpsOnlyFilter)
  override def filters = csrfFilter +: map.foldRight[Seq[EssentialFilter]](Seq.empty) { case ((k,f),filters) => 
    configuration.getBoolean(k) collect {
      case true => f +: filters
    } getOrElse filters
  }
}
