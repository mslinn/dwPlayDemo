package module

import com.google.inject.{AbstractModule, Provides}

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1.TwitterProvider

import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{CookieSecretProvider, CookieSecretSettings}
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.repositories.DelegableAuthInfoRepository
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.impl.services.GravatarService

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule

import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer.MailerClient
import play.api.libs.ws.WSClient

import daos._
import models.User
import services.{UserService,UserTokenService}
import utils.Mailer

class Module extends AbstractModule with ScalaModule {

  def configure() {
    bind[IdentityService[User]].to[UserService]
    bind[UserDao].to[MongoUserDao]
    bind[UserTokenDao].to[MongoUserTokenDao]
    bind[DelegableAuthInfoDAO[PasswordInfo]].to[PasswordInfoDao]
    bind[DelegableAuthInfoDAO[OAuth1Info]].to[OAuth1InfoDao]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  @Provides
  def provideMailer(configuration:Configuration, mailerClient:MailerClient) = 
    new Mailer(configuration, mailerClient)

  @Provides
  def provideEnvironment(
    identityService: IdentityService[User],
    authenticatorService: AuthenticatorService[CookieAuthenticator],
    eventBus: EventBus): Environment[User, CookieAuthenticator] = {

    Environment[User, CookieAuthenticator](
      identityService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides
  def provideAuthenticatorService(
    fingerprintGenerator: FingerprintGenerator,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock): AuthenticatorService[CookieAuthenticator] = {

    val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
    new CookieAuthenticatorService(config, None, fingerprintGenerator, idGenerator, clock)
  }

  @Provides
  def provideSocialProviderRegistry(twitterProvider: TwitterProvider): SocialProviderRegistry = {
    SocialProviderRegistry(Seq(twitterProvider))
  }

  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository,
    passwordHasher: PasswordHasher): CredentialsProvider = {

    new CredentialsProvider(authInfoRepository, passwordHasher, Seq(passwordHasher))
  }

  @Provides
  def provideAuthInfoRepository(
    passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo],
    oauth1InfoDAO: DelegableAuthInfoDAO[OAuth1Info]): AuthInfoRepository = {

    new DelegableAuthInfoRepository(passwordInfoDAO, oauth1InfoDAO)
  }

  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)

  @Provides
  def provideTwitterProvider(
    httpLayer: HTTPLayer,
    tokenSecretProvider: OAuth1TokenSecretProvider,
    configuration: Configuration): TwitterProvider = {

    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.twitter")
    new TwitterProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
  }

  @Provides
  def provideOAuth1TokenSecretProvider(configuration: Configuration, clock: Clock): OAuth1TokenSecretProvider = {
    val settings = configuration.underlying.as[CookieSecretSettings]("silhouette.oauth1TokenSecretProvider")
    new CookieSecretProvider(settings, clock)
  }
}
