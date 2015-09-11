package daos

import com.mohiva.play.silhouette.api.LoginInfo

import scala.concurrent.Await

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable.Specification

class UserDaoSpec extends Specification with DaoSpecResources {

  "UserDao" should {
    "save users and find them by userId" in withUserDao { userDao =>
      val future = for {
        _ <- userDao.save(credentialsTestUser)
        maybeUser <- userDao.find(credentialsTestUser.id)
      } yield maybeUser.map(_ == credentialsTestUser)
      Await.result(future, timeout) must beSome(true)
    }

    "save users and find them by loginInfo" in withUserDao { userDao =>
      val future = for {
        _ <- userDao.save(credentialsTestUser)
        maybeUser <- userDao.find(LoginInfo("credentials","john.doe@gmail.com"))
      } yield maybeUser.map(_ == credentialsTestUser)
      Await.result(future, timeout) must beSome(true)
    }

    "confirm a profile" in withUserDao { userDao =>
      val future = for {
        user <- userDao.save(credentialsTestUser)
        user <- userDao.confirm(loginInfo)
      } yield user
      val user = Await.result(future, timeout)
      user.profiles(0).confirmed === true
    }

    "link a new profile to an existing user" in withUserDao { userDao =>
      val future = for {
        user <- userDao.save(credentialsTestUser)
        user <- userDao.link(user, testOauth1Profile)
      } yield user
      val user = Await.result(future, timeout)
      user.profiles.length mustEqual 2
      user.profiles.map(_.loginInfo) must contain(loginInfo, oauth1LoginInfo) 
    }

    "update an existing profile" in withUserDao { userDao =>
      val copy = testPasswordProfile.copy(firstName = Some("Johnny"))
      val future = for {
        user <- userDao.save(credentialsTestUser)
        user <- userDao.update(copy)
      } yield user
      val user = Await.result(future, timeout)
      user.profiles(0).firstName === Some("Johnny")
    }
  }
}
