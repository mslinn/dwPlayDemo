package daos

import scala.concurrent.Await

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable.Specification

class PasswordInfoDaoSpec extends Specification with DaoSpecResources {

  "PasswordInfoDao" should {
    "Add new credentials information" in withPasswordInfoDao { passwordInfoDao =>
      val future = for {
        _ <- passwordInfoDao.save(loginInfo, passwordInfo)
        maybePasswordInfo <- passwordInfoDao.find(loginInfo)
      } yield maybePasswordInfo.map(_ == passwordInfo)
      Await.result(future, timeout) must beSome(true)
    }

    "Remove existing credentials information" in withPasswordInfoDao { passwordInfoDao =>
      val future = for {
        _ <- passwordInfoDao.save(loginInfo, passwordInfo)
        _ <- passwordInfoDao.remove(loginInfo)
        maybePasswordInfo <- passwordInfoDao.find(loginInfo)
      } yield maybePasswordInfo
      Await.result(future, timeout) must beNone       
    }
  }
}
