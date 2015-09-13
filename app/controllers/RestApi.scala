package controllers

import javax.inject.Inject

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.{Environment,Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.i18n.{I18nSupport,MessagesApi,Messages}

import models.User, User._

class RestApi @Inject() (
  val messagesApi: MessagesApi, 
  val env:Environment[User,CookieAuthenticator]) extends Silhouette[User,CookieAuthenticator] {

  def profile = SecuredAction.async { implicit request =>
    val json = Json.toJson(request.identity.profileFor(request.authenticator.loginInfo).get)
    val prunedJson = json.transform(
      (__ \ 'loginInfo).json.prune andThen 
      (__ \ 'passordInfo).json.prune andThen 
      (__ \ 'oauth1Info).json.prune)
    prunedJson.fold(
      _ => Future.successful(InternalServerError(Json.obj("error" -> Messages("error.profileError")))),
      js => Future.successful(Ok(js))
    )
  }

  override def onNotAuthenticated(request:RequestHeader) = {
    Some(Future.successful(Unauthorized(Json.obj("error" -> Messages("error.profileUnauth")))))
  }
}
