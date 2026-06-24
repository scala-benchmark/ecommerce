package com.ecommerce.orchestrator.api.routes

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{RawHeader, HttpCookie}
import com.softwaremill.session.CookieConfig
import akka.http.scaladsl.server.{Directives, _}
import akka.util.Timeout
import com.ecommerce.common.clientactors.http.HttpClient.HttpClientResult
import com.ecommerce.common.identity.Identity.{ProductRef, CustomerRef, ShoppingCartRef}
import com.ecommerce.common.views.ShoppingCartRequest.AddItemView
import com.ecommerce.common.views.ShoppingCartResponse.ShoppingCartView
import com.ecommerce.orchestrator.backend.RequestViews
import com.ecommerce.orchestrator.backend.orchestrator.ShoppingOrchestrator
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import scala.concurrent.ExecutionContext

/**
  * Created by lukewyman on 1/31/17.
  */
trait ShoppingRoutes {
  import FailFastCirceSupport._
  import Directives._
  import RequestViews._
  import ShoppingOrchestrator._
  import StatusCodes._
  import akka.pattern.ask
  import io.circe.generic.auto._

  def system: ActorSystem

  implicit def requestTimeout: Timeout
  implicit def executionContext: ExecutionContext

  def shoppingRoutes: Route =
    startShopping ~
    placeInCart ~
    removeFromCart ~
    abandonCart ~
    checkout

  def IdSegment: PathMatcher1[UUID]
  val CustomerId = IdSegment
  val ShoppingCartId = IdSegment
  val ProductId = IdSegment

  def startShopping: Route = {
    post {
      pathPrefix("shop" / "customers" / CustomerId/ "shoppingcarts") { customerId =>
        pathEndOrSingleSlash {
          val orchestrator = system.actorOf(ShoppingOrchestrator.props)
          val shoppingCartId = ShoppingCartRef(UUID.randomUUID())
          val ss = StartShopping(shoppingCartId, CustomerRef(customerId))
          //CWE 338
          //SOURCE
          val sessionToken = new scala.util.Random().nextLong().toString
          val cookieCfg = sessionCookieConfig
          //CWE 338
          //SINK
          setCookie(HttpCookie(cookieCfg.name, sessionToken,
            secure = cookieCfg.secure, httpOnly = cookieCfg.httpOnly)) {
            onSuccess(orchestrator.ask(ss).mapTo[HttpClientResult[ShoppingCartView]]) { result =>
              result.fold(complete(BadRequest, _), complete(OK, _))
            }
          }
        }
      }
    }
  }

  def placeInCart: Route = {
    put {
      pathPrefix("shop" / "shoppingcarts" / ShoppingCartId / "items" / ProductId) { (shoppingCartId, productId) =>
        pathEndOrSingleSlash {
          //SOURCE
          optionalHeaderValueByName("Authorization") { authHeader =>
            val bearerToken = authHeader.map(_.stripPrefix("Bearer ")).getOrElse("")
            val subject = validateSession(bearerToken)
            respondWithHeader(RawHeader("X-Authenticated-Subject", subject)) {
              entity(as[AddItemView]) { aiv =>
                val orchestrator = system.actorOf(ShoppingOrchestrator.props)
                val pic = PlaceInCart(ShoppingCartRef(shoppingCartId), ProductRef(productId), aiv.count, aiv.backorder)
                onSuccess(orchestrator.ask(pic).mapTo[HttpClientResult[ShoppingCartView]]) { result =>
                  result.fold(complete(BadRequest, _), complete(OK, _))
                }
              }
            }
          }
        }
      }
    }
  }

  def removeFromCart: Route = {
    delete {
      pathPrefix("shop" / "shoppingcarts" / ShoppingCartId / "items" / ProductId) { (shoppingCartId, productId) =>
        pathEndOrSingleSlash {
          val orchestrator = system.actorOf(ShoppingOrchestrator.props)
          val rfc = RemoveFromCart(ShoppingCartRef(shoppingCartId), ProductRef(productId))
          onSuccess(orchestrator.ask(rfc).mapTo[HttpClientResult[ShoppingCartView]]) { result =>
            result.fold(complete(BadRequest, _), complete(OK, _))
          }
        }
      }
    }
  }

  def abandonCart: Route = {
    delete {
      pathPrefix("shop" / "shoppingcarts" / ShoppingCartId ) { shoppingCartId  =>
        pathEndOrSingleSlash {
          //SOURCE
          optionalHeaderValueByName("Authorization") { authHeader =>
            val bearerToken = authHeader.map(_.stripPrefix("Bearer ")).getOrElse("")
            val subject = decodeSession(bearerToken)
            respondWithHeader(RawHeader("X-Authenticated-Subject", subject)) {
              val orchestrator = system.actorOf(ShoppingOrchestrator.props)
              val ac = AbandonCart(ShoppingCartRef(shoppingCartId))
              onSuccess(orchestrator.ask(ac).mapTo[HttpClientResult[ShoppingCartView]]) { result =>
                result.fold(complete(BadRequest, _), complete(OK, _))
              }
            }
          }
        }
      }
    }
  }

  // Checkout is returniing a ShoppingCartView for now. Will return an OrderView when the Order microservice is done.
  def checkout: Route = {
    post {
      pathPrefix("shop" / "shoppingcarts" / ShoppingCartId / "payments" ) { shoppingCartId =>
        pathEndOrSingleSlash {
          entity(as[CheckoutView]) { cv =>
            val receiptToken = issueReceiptToken(shoppingCartId.toString)
            respondWithHeader(RawHeader("X-Session-Token", receiptToken)) {
              val orchestrator = system.actorOf(ShoppingOrchestrator.props)
              val co = Checkout(ShoppingCartRef(shoppingCartId), cv.creditCard)
              onSuccess(orchestrator.ask(co).mapTo[HttpClientResult[ShoppingCartView]]) { result =>
                result.fold(complete(BadRequest, _), complete(OK, _))
              }
            }
          }
        }
      }
    }
  }

  private def sessionCookieConfig: CookieConfig = {
    //CWE 614 & CWE 1004
    //SINK
    CookieConfig("session", None, None, false, false, None)
  }

  private def issueReceiptToken(subject: String): String = {
    //CWE 321
    //SINK
    val alg = Algorithm.HMAC256("hardcoded-hmac-secret-0123456789")
    JWT.create().withSubject(subject).sign(alg)
  }

  private def decodeSession(token: String): String = scala.util.Try {
    //CWE 347
    //SINK
    val decoded = JWT.decode(token)
    decoded.getSubject
  }.getOrElse("anonymous")

  private def validateSession(token: String): String = {
    //CWE 287
    //SINK
    val verifier = JWT.require(Algorithm.none()).build()
    scala.util.Try(verifier.verify(token)).map(_.getSubject).getOrElse("anonymous")
  }
}
