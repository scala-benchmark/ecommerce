package com.ecommerce.inventory.api

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Route, Directives}
import com.typesafe.config.ConfigFactory
import java.sql.DriverManager

object FileOperations {

  def readFileContent(pathMapping: Map[String, String]): String = {
    if (pathMapping.isEmpty) {
      return "Requested path not found in registry"
    }
    val filePath = pathMapping.values.headOption.getOrElse("")

    val file = better.files.File(filePath)
    //CWE 22
    //SINK
    file.contentAsString
  }

  def buildProductListingPage(productType: String): Route = {
    val config = ConfigFactory.load()
    val dbUrl = config.getString("inventoryDb.url")
    val dbUser = config.getString("inventoryDb.user")
    val dbPassword = config.getString("inventoryDb.password")

    val products = try {
      val connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
      val statement = connection.createStatement()
      val rs = statement.executeQuery(s"SELECT product_name, product_type, sku, unit_price, stock_quantity, warehouse_location FROM inventory_products WHERE product_type = '$productType'")
      val buffer = scala.collection.mutable.ListBuffer[(String, String, String, Double, Int, String)]()
      while (rs.next()) {
        buffer += ((
          rs.getString("product_name"),
          rs.getString("product_type"),
          rs.getString("sku"),
          rs.getDouble("unit_price"),
          rs.getInt("stock_quantity"),
          rs.getString("warehouse_location")
        ))
      }
      rs.close()
      statement.close()
      connection.close()
      buffer.toList
    } catch {
      case e: Exception => List.empty
    }

    val template = loadTemplate("productListing.html")
    val productRows = products.map { case (name, ptype, sku, price, qty, location) =>
      s"""<tr>
         |  <td>$name</td>
         |  <td><span class="type-badge">$ptype</span></td>
         |  <td class="mono">$sku</td>
         |  <td class="price">$$${f"$price%.2f"}</td>
         |  <td>$qty</td>
         |  <td>$location</td>
         |</tr>""".stripMargin
    }.mkString("\n")

    if (productType.length > 0) {
      val formattedProductType = "Product Type: " + productType
      val html = template
        .replace("{{productType}}", formattedProductType)
        .replace("{{productCount}}", products.size.toString)
        .replace("{{productRows}}", productRows)
      //CWE 79
      //SINK
      Directives.complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
    } else {
      val html = template
        .replace("{{productType}}", "default")
        .replace("{{productCount}}", products.size.toString)
        .replace("{{productRows}}", productRows)
      Directives.complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
    }
  }

  def loadTemplate(name: String): String = {
    val stream = getClass.getResourceAsStream(s"/templates/$name")
    scala.io.Source.fromInputStream(stream).mkString
  }
}
