package daos

import scala.concurrent.Await

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test._
import play.api.test.Helpers._

import org.joda.time.DateTime
import org.specs2.mutable.Specification

import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import models.UserToken

class UserTokenDaoSpec extends Specification {

  val timeout = DurationInt(10).seconds
  def fakeApp = FakeApplication(additionalConfiguration = Map("mongodb.uri" -> "mongodb://localhost:27017/test"))

  def withUserTokenDao[T](t:UserTokenDao => T):T = running(fakeApp) {
    val userTokenDao = new MongoUserTokenDao
    Await.ready(userTokenDao.tokens.drop(), timeout)
    t(userTokenDao)
  }

  val token = UserToken(id=UUID.randomUUID(), userId=UUID.randomUUID(), "john.doe@gmail.com", new DateTime(), true)

  "UserTokenDao" should {
    "Persist and find a token" in withUserTokenDao { userTokenDao =>
      val future = for {
        _ <- userTokenDao.save(token)
        maybeToken <- userTokenDao.find(token.id)
      } yield maybeToken.map(_ == token)
      Await.result(future, timeout) must beSome(true)
    }

    "Remove a token" in withUserTokenDao { userTokenDao =>
      val future = for {
        _ <- userTokenDao.save(token)
        _ <- userTokenDao.remove(token.id)
        maybeToken <- userTokenDao.find(token.id)
      } yield maybeToken
      Await.result(future, timeout) must beNone       
    }
  }
}
