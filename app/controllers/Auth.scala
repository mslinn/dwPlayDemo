package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._

import net.ceedubs.ficus.Ficus._

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.{Environment,LoginInfo,Silhouette}
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{Credentials,PasswordHasher}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._

import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc._
import play.api.i18n.{I18nSupport,MessagesApi,Messages}
import play.api.libs.concurrent.Execution.Implicits._

import models.{Profile,User,UserToken}
import services.{UserService,UserTokenService}
import utils.Mailer

import org.joda.time.DateTime

object AuthForms {

  // Sign up
  case class SignUpData(email:String, password:String, firstName:String, lastName:String)
  def signUpForm(implicit messages:Messages) = Form(mapping(
      "email" -> email,
      "password" -> tuple(
        "password1" -> nonEmptyText.verifying(minLength(6)),
        "password2" -> nonEmptyText
      ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2),
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText
    )
    ((email, password, firstName, lastName) => SignUpData(email, password._1, firstName, lastName))
    (signUpData => Some((signUpData.email, ("",""), signUpData.firstName, signUpData.lastName)))
  )

  // Sign in
  case class SignInData(email:String, password:String, rememberMe:Boolean)
  val signInForm = Form(mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )(SignInData.apply)(SignInData.unapply)
  )

  // Start password recovery
  val emailForm = Form(single("email" -> email))

  // Passord recovery
  def resetPasswordForm(implicit messages:Messages) = Form(tuple(
    "password1" -> nonEmptyText.verifying(minLength(6)),
    "password2" -> nonEmptyText
  ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2))
}

class Auth @Inject() (
  val messagesApi: MessagesApi, 
  val env:Environment[User,CookieAuthenticator],
  socialProviderRegistry: SocialProviderRegistry,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  userService: UserService,
  userTokenService: UserTokenService,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  configuration: Configuration,
  mailer: Mailer) extends Silhouette[User,CookieAuthenticator] {

  import AuthForms._

  def startSignUp = UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(user) => Redirect(routes.Application.index)
      case None => Ok(views.html.auth.startSignUp(signUpForm))
    })
  }

  def handleStartSignUp = Action.async { implicit request =>
    signUpForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.auth.startSignUp(bogusForm))),
      signUpData => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, signUpData.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(_) => 
            Future.successful(Redirect(routes.Auth.startSignUp()).flashing(
              "error" -> Messages("error.userExists", signUpData.email)))
          case None => 
            val profile = Profile(
              loginInfo = loginInfo, 
              confirmed = false,
              email = Some(signUpData.email), 
              firstName = Some(signUpData.firstName), 
              lastName = Some(signUpData.lastName), 
              fullName = Some(s"${signUpData.firstName} ${signUpData.lastName}"),
              passwordInfo = None, 
              oauth1Info = None,
              avatarUrl = None)
            for {
              avatarUrl <- avatarService.retrieveURL(signUpData.email)
              user <- userService.save(User(id = UUID.randomUUID(), profiles = List(profile.copy(avatarUrl = avatarUrl))))
              _ <- authInfoRepository.add(loginInfo, passwordHasher.hash(signUpData.password))
              token <- userTokenService.save(UserToken.create(user.id, signUpData.email, true))
            } yield {
              mailer.welcome(profile, link = routes.Auth.signUp(token.id.toString).absoluteURL())
              Ok(views.html.auth.finishSignUp(profile))
            }
        }
      }
    )
  }

  def signUp(tokenId:String) = Action.async { implicit request =>
    val id = UUID.fromString(tokenId)
    userTokenService.find(id).flatMap {
      case None => 
        Future.successful(NotFound(views.html.errors.notFound(request)))
      case Some(token) if token.isSignUp && !token.isExpired => 
        userService.find(token.userId).flatMap {
          case None => Future.failed(new IdentityNotFoundException(Messages("error.noUser")))
          case Some(user) => 
            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
            for {
              authenticator <- env.authenticatorService.create(loginInfo)
              value <- env.authenticatorService.init(authenticator)
              _ <- userService.confirm(loginInfo)
              _ <- userTokenService.remove(id)
              result <- env.authenticatorService.embed(value, Redirect(routes.Application.index()))
            } yield result
        }
      case Some(token) => 
        userTokenService.remove(id).map {_ => NotFound(views.html.errors.notFound(request))}
    }
  }

  def signIn = UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(user) => Redirect(routes.Application.index())
      case None => Ok(views.html.auth.signIn(signInForm,socialProviderRegistry))
    })
  }

  def authenticate = Action.async { implicit request =>
    signInForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.auth.signIn(bogusForm, socialProviderRegistry))),
      signInData => {
        val credentials = Credentials(signInData.email, signInData.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo => 
          userService.retrieve(loginInfo).flatMap {
            case None => 
              Future.successful(Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.noUser")))
            case Some(user) if !user.profileFor(loginInfo).map(_.confirmed).getOrElse(false) => 
              Future.successful(Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.unregistered", signInData.email)))
            case Some(_) => for {
              authenticator <- env.authenticatorService.create(loginInfo).map { 
                case authenticator if signInData.rememberMe =>
                  val c = configuration.underlying
                  authenticator.copy(
                    expirationDateTime = new DateTime() + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                  )
                case authenticator => authenticator
              }
              value <- env.authenticatorService.init(authenticator)
              result <- env.authenticatorService.embed(value, Redirect(routes.Application.index()))
            } yield result
          }
        }.recover {
          case e:ProviderException => Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.invalidCredentials"))
        }
      }
    )
  }

  def signOut = SecuredAction.async { implicit request =>
    env.authenticatorService.discard(request.authenticator, Redirect(routes.Application.index()))
  }

  def startResetPassword = Action { implicit request =>
    Ok(views.html.auth.startResetPassword(emailForm))
  }

  def handleStartResetPassword = Action.async { implicit request =>
    emailForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.auth.startResetPassword(bogusForm))),
      email => userService.retrieve(LoginInfo(CredentialsProvider.ID, email)).flatMap {
        case None => Future.successful(Redirect(routes.Auth.startResetPassword()).flashing("error" -> Messages("error.noUser")))
        case Some(user) => for {
          token <- userTokenService.save(UserToken.create(user.id, email, isSignUp = false))
        } yield {
          mailer.resetPassword(email, link = routes.Auth.resetPassword(token.id.toString).absoluteURL())
          Ok(views.html.auth.resetPasswordInstructions(email))
        }
      }
    )
  }

  def resetPassword(tokenId:String) = Action.async { implicit request =>
    val id = UUID.fromString(tokenId)
    userTokenService.find(id).flatMap {
      case None => 
        Future.successful(NotFound(views.html.errors.notFound(request)))
      case Some(token) if !token.isSignUp && !token.isExpired => 
        Future.successful(Ok(views.html.auth.resetPassword(tokenId, resetPasswordForm)))
      case _ => for {
        _ <- userTokenService.remove(id)
      } yield NotFound(views.html.errors.notFound(request))
    }
  }

  def handleResetPassword(tokenId:String) = Action.async { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.auth.resetPassword(tokenId, bogusForm))),
      passwords => {
        val id = UUID.fromString(tokenId)
        userTokenService.find(id).flatMap {
          case None => 
            Future.successful(NotFound(views.html.errors.notFound(request)))
          case Some(token) if !token.isSignUp && !token.isExpired =>
            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
            for {
              _ <- authInfoRepository.save(loginInfo, passwordHasher.hash(passwords._1))
              authenticator <- env.authenticatorService.create(loginInfo)
              value <- env.authenticatorService.init(authenticator)
              _ <- userTokenService.remove(id)
              result <- env.authenticatorService.embed(value, Ok(views.html.auth.resetPasswordDone()))
            } yield result
        }
      } 
    )
  }

  def socialAuthenticate(providerId:String) = UserAwareAction.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](providerId) match {
      case Some(p:SocialProvider with CommonSocialProfileBuilder) => p.authenticate.flatMap {
        case Left(result) => Future.successful(result)
        case Right(authInfo) => for {
          profile <- p.retrieveProfile(authInfo)
          user <- request.identity.fold(userService.save(profile))(userService.link(_,profile))
          authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
          authenticator <- env.authenticatorService.create(profile.loginInfo)
          value <- env.authenticatorService.init(authenticator)
          result <- env.authenticatorService.embed(value, Redirect(routes.Application.index()))
        } yield result
      } 
      case _ => Future.successful(
        Redirect(request.identity.fold(routes.Auth.signIn())(_ => routes.Application.profile())).flashing(
          "error" -> Messages("error.noProvider", providerId))
      )
    }).recover {
      case e:ProviderException => 
        logger.error("Provider error", e)
        Redirect(request.identity.fold(routes.Auth.signIn())(_ => routes.Application.profile()))
          .flashing("error" -> Messages("error.notAuthenticated", providerId))
    }
  }
}
