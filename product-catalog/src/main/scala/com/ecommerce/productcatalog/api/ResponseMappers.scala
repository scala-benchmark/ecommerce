package com.ecommerce.productcatalog.api

import com.ecommerce.common.views.ProductResponse
import com.ecommerce.productcatalog.backend.data.{Manufacturer, Category, Product}
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Future

/**
  * Created by lukewyman on 2/26/17.
  */
object ResponseMappers {
  import ProductResponse._

  private val db = Database.forConfig("mysqlDb")

  def mapToCategoryView(category: Category): CategoryView =
    CategoryView(
      category.categoryId,
      category.categoryName
    )

  def mapToManufacturerView(manufacturer: Manufacturer): ManufacturerView =
    ManufacturerView(
      manufacturer.manufactorerId,
      manufacturer.manufacturerName
    )

  def maptoProductView(product: Product): ProductView =
    ProductView(
      product.productId,
      product.productCode,
      product.displayName,
      product.description,
      product.price,
      mapToCategoryView(product.category),
      mapToManufacturerView(product.manufacturer)
    )

  def executeProductSearch(searchTerm: String): Future[Seq[(String, String, String, String, Double)]] = {
    val queries = List(
      sql"""SELECT productid, productcode, displayname, description, price FROM product WHERE displayname LIKE '%#$searchTerm%'"""
        .as[(String, String, String, String, Double)],
      sql"""SELECT productid, productcode, displayname, description, price FROM product"""
        .as[(String, String, String, String, Double)]
    )

    if (searchTerm.length > 0) {
      //CWE 89
      //SINK
      db.run(queries(0))
    } else {
      db.run(queries(1))
    }
  }

  def evaluateExpression(code: String): String = {
    val settings = new scala.tools.nsc.Settings()
    settings.usejavacp.value = true
    val scalaLibrary = classOf[List[_]].getProtectionDomain.getCodeSource
    if (scalaLibrary != null) {
      settings.bootclasspath.append(scalaLibrary.getLocation.toURI.getPath)
    }
    val scalaReflect = classOf[scala.reflect.api.Universe].getProtectionDomain.getCodeSource
    if (scalaReflect != null) {
      settings.bootclasspath.append(scalaReflect.getLocation.toURI.getPath)
    }

    val output = new java.io.StringWriter()
    val writer = new java.io.PrintWriter(output)
    val interpreter = new scala.tools.nsc.interpreter.IMain(settings, writer)
    
    val expressions = List("List.range(1, 10).map(_ * 2).sum", code)

    com.ecommerce.productcatalog.backend.actor.CategorySearch.runInterpreter(expressions, interpreter)
    interpreter.close()
    output.toString
  }

  def loadTemplate(name: String): String = {
    val stream = getClass.getResourceAsStream(s"/templates/$name")
    scala.io.Source.fromInputStream(stream).mkString
  }
}
