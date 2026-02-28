package com.ecommerce.productcatalog.api

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server._
import akka.util.Timeout
import com.ecommerce.common.clientactors.protocols.ProductProtocol._
import com.ecommerce.common.identity.Identity.{ManufacturerRef, CategoryRef, ProductRef}
import com.ecommerce.productcatalog.backend.actor.{ManufacturerSearch, CategorySearch, ProductSearch}
import com.ecommerce.productcatalog.backend.data.{Product, Category, Manufacturer}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.ExecutionContext
import scala.util.Try

class ProductService(val system: ActorSystem, val requestTimeout: Timeout) extends ProductRoutes {
  val executionContext = system.dispatcher
}

trait ProductRoutes {

  import Directives._
  import StatusCodes._
  import FailFastCirceSupport._
  import io.circe.generic.auto._
  import akka.pattern.ask
  import ResponseMappers._

  def system: ActorSystem

  implicit def requestTimeout: Timeout
  implicit def executionContext: ExecutionContext

  def routes: Route =
    getProductByProductId ~
    getProductsBySearchString ~
    getProductsByCategoryId ~
    getCategoryById ~
    getCategories ~
    getProductsByManufacturerId ~
    getManufacturerById ~
    getManufacturers ~
    searchCatalog ~
    evaluateFormula

  def getProductByProductId: Route = {
    get {
      pathPrefix("products" / ProductId ) { productId =>
        pathEndOrSingleSlash {
          val productSearch = system.actorOf(ProductSearch.props)
          onSuccess(productSearch.ask(GetProductByProductId(ProductRef(productId))).mapTo[Product]) { p =>
            complete(OK, maptoProductView(p))
          }
        }
      }
    }
  }

  //TODO: Figure out how to do query parameters with Akka HTTP
  def getProductsBySearchString: Route = {
    get {
      pathPrefix("products") {
        pathEndOrSingleSlash {
          complete(OK)
        }
      }
    }
  }

  def getProductsByCategoryId: Route = {
    get {
      pathPrefix("categories" / CategoryId / "products") { categoryId =>
        pathEndOrSingleSlash {
          val productSearch = system.actorOf(ProductSearch.props)
          onSuccess(productSearch.ask(GetProductsByCategory(CategoryRef(categoryId))).mapTo[List[Product]]) { ps =>
            complete(OK, ps.map(maptoProductView(_)))
          }
        }
      }
    }
  }

  def getCategoryById: Route = {
    get {
      pathPrefix("categories" / CategoryId) { categoryId =>
        pathEndOrSingleSlash {
          val categorySearch = system.actorOf(CategorySearch.props)
          onSuccess(categorySearch.ask(GetCategoryById(CategoryRef(categoryId))).mapTo[Category]) { c =>
            complete(OK, mapToCategoryView(c))
          }
        }
      }
    }
  }

  def getCategories: Route = {
    get {
      pathPrefix("categories") {
        pathEndOrSingleSlash {
          val categorySearch = system.actorOf(CategorySearch.props)
          onSuccess(categorySearch.ask(GetCategories).mapTo[List[Category]]) { cs =>
            complete(OK, cs.map(mapToCategoryView(_)))
          }
        }
      }
    }
  }

  def getProductsByManufacturerId: Route = {
    get {
      pathPrefix("manufacturers" / ManufacturerId / "products") { manufacturerId =>
        pathEndOrSingleSlash {
          val productSearch = system.actorOf(ProductSearch.props)
          onSuccess(productSearch.ask(GetProductsByManufacturer(ManufacturerRef(manufacturerId))).mapTo[List[Product]]) { ps =>
            complete(OK, ps.map(maptoProductView(_)))
          }
        }
      }
    }
  }

  def getManufacturerById: Route = {
    get {
      pathPrefix("manufacturers" / ManufacturerId) { manufacturerId =>
        pathEndOrSingleSlash {
          val manufacturerSearch = system.actorOf(ManufacturerSearch.props)
          onSuccess(manufacturerSearch.ask(GetManufacturerById(ManufacturerRef(manufacturerId))).mapTo[Manufacturer]) { m =>
            complete(OK, mapToManufacturerView(m))
          }
        }
      }
    }
  }

  def getManufacturers: Route = {
    get {
      pathPrefix("manufacturers") {
        pathEndOrSingleSlash {
          val manufacturerSeach = system.actorOf(ManufacturerSearch.props)
          onSuccess(manufacturerSeach.ask(GetManufacturers).mapTo[List[Manufacturer]]) { ms =>
            complete(OK, ms.map(mapToManufacturerView(_)))
          }
        }
      }
    }
  }

  def searchCatalog: Route = {
    get {
      pathPrefix("catalog" / "search") {
        pathEndOrSingleSlash {
          //CWE 89
          //SOURCE
          parameter("q") { searchQuery =>
            val sanitizedTerm = QueryValidation.validateSearchTerm(searchQuery)
            if (sanitizedTerm == "Invalid search term") {
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                s"<html><body><h1>Error</h1><p>The specified search term is not valid.</p></body></html>"))
            } 

            val resultsFuture = ResponseMappers.executeProductSearch(sanitizedTerm)
            onSuccess(resultsFuture) { products =>
              val template = ResponseMappers.loadTemplate("catalogSearch.html")
              val productCards = if (products.isEmpty) {
                """<div class="empty-state"><h2>No products found</h2><p>Try a different search term</p></div>"""
              } else {
                products.map { case (id, code, name, desc, price) =>
                  val eName = name.replace("&", "&amp;").replace("<", "&lt;")
                  val eDesc = desc.replace("&", "&amp;").replace("<", "&lt;")
                  s"""<div class="product-card">
                     |  <div>
                     |    <div class="product-name">$eName</div>
                     |    <span class="product-code">$code</span>
                     |    <p class="product-desc">$eDesc</p>
                     |  </div>
                     |  <div class="product-price">$$${f"$price%.2f"}</div>
                     |</div>""".stripMargin
                }.mkString("\n")
              }
              val html = template
                .replace("{{searchTerm}}", sanitizedTerm)
                .replace("{{resultCount}}", products.length.toString)
                .replace("{{productRows}}", productCards)
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
            }
          }
        }
      }
    }
  }

  def evaluateFormula: Route = {
    post {
      pathPrefix("catalog" / "formula" / "evaluate") {
        pathEndOrSingleSlash {
          //CWE 94
          //SOURCE
          extractStrictEntity(scala.concurrent.duration.Duration(5, "seconds")) { entity =>
            val expression = entity.data.utf8String
            val validSyntax = QueryValidation.validateExpressionSyntax(expression)
            val checkedComplexity = QueryValidation.checkExpressionComplexity(validSyntax)

            val (output, success) = try {
              val result = ResponseMappers.evaluateExpression(checkedComplexity)
              (result, true)
            } catch {
              case e: Exception =>
                (e.getMessage, false)
            }
            val jsonResponse = s"""{"expression":"${checkedComplexity.replace("\"", "\\\"")}","success":$success,"output":"${output.replace("\"", "\\\"").replace("\n", "\\n")}"}"""
            complete(HttpEntity(ContentTypes.`application/json`, jsonResponse))
          }
        }
      }
    }
  }

  val IdSegment = Segment.flatMap(id => Try(UUID.fromString(id)).toOption)
  val ProductId = IdSegment
  val CategoryId = IdSegment
  val ManufacturerId = IdSegment
}
