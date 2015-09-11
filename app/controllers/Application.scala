package controllers

import javax.inject.Inject

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.{Environment,Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry

import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport,MessagesApi}

import models.User

class Application @Inject() (
  val messagesApi: MessagesApi, 
  val env:Environment[User,CookieAuthenticator],
  socialProviderRegistry: SocialProviderRegistry) extends Silhouette[User,CookieAuthenticator] {

  def index = UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.index(request.identity, request.authenticator.map(_.loginInfo))))
  }

  def profile = SecuredAction { implicit request =>
    Ok(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry))
  }
}
