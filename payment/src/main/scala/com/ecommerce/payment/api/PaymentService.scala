package com.ecommerce.payment.api

import akka.actor.ActorSystem
import akka.util.Timeout
import akka.http.scaladsl.server.{Route, Directives}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.ExecutionContext
import scala.tools.reflect.ToolBox
import scala.reflect.runtime.currentMirror

import zio._
import zio.jdbc._
import scala.concurrent.duration._

/**
  * Created by lukewyman on 2/5/17.
  */
class PaymentService(val system: ActorSystem, val requestTimeout: Timeout) extends PaymentRoutes {
  val executionContext = system.dispatcher
}

trait PaymentRoutes {
  import Directives._
  import StatusCodes._
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  def routes: Route = pay ~ searchTransactions ~ calculateDiscount

  private def validateSearchQuery(query: String): String = {
    if (query == null || query.isEmpty) {
      println("Warning: Search query is empty")
    }
    query
  }

  private def validateQueryLength(query: String): String = {
    if (query.length > 5000) {
      println("Warning: Query is very long")
    }
    query
  }

  private def validateCodeInput(code: String): String = {
    if (code == null || code.isEmpty) {
      println("Warning: Code input is empty")
    }
    code
  }

  private def validateCodeSyntax(code: String): String = {
    if (!code.contains("(") && !code.contains("{")) {
      println("Warning: Code may not be valid Scala")
    }
    code
  }

  def pay: Route = {
    post {
      pathPrefix("payments") {
        pathEndOrSingleSlash {
          complete(OK)
        }
      }
    }
  }

  def searchTransactions: Route = {
    get {
      pathPrefix("payments" / "transactions" / "search") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("query") { rawQuery =>
            val validatedOnce = validateSearchQuery(rawQuery)
            val searchQuery = validateQueryLength(validatedOnce)

            val results = try {
          
              val unsafeQuery = s"SELECT id, description, amount FROM transactions WHERE description LIKE '%$searchQuery%' OR customer_name LIKE '%$searchQuery%'"

              val poolLayer = ZConnectionPool.postgres(
                host = "localhost",
                port = 5433,
                database = "payments",
                props = Map(
                  "user" -> "postgres",
                  "password" -> "postgres"
                )
              )

              val rawSqlFragment: SqlFragment = SqlFragment(unsafeQuery)
              
              val queryEffect: ZIO[ZConnectionPool, Throwable, Chunk[(String, String, String)]] = 
                transaction {
                  //CWE 89
                  //SINK
                  rawSqlFragment.query[(String, String, String)].selectAll
                }

              val runtime = Runtime.default
              Unsafe.unsafe { implicit unsafe =>
                runtime.unsafe.run(
                  queryEffect
                    .provideLayer(ZLayer.succeed(ZConnectionPoolConfig.default) >>> poolLayer)
                    .timeout(zio.Duration.fromSeconds(10))
                    .map(_.getOrElse(Chunk.empty))
                ).getOrThrow().toList
              }
            } catch {
              case e: Exception =>
                List(("Error", e.getMessage, "N/A"))
            }

            val escapedQuery = searchQuery.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val tableRows = results.map { case (id, desc, amount) =>
              s"""<tr><td>${id.replace("<", "&lt;")}</td><td>${desc.replace("<", "&lt;")}</td><td>$amount</td></tr>"""
            }.mkString("\n")

            val htmlContent = s"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Transaction Search - Payment System</title>
  <style>
    :root { --bg: #0f172a; --card: #1e293b; --accent: #10b981; --text: #e2e8f0; --muted: #94a3b8; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Inter', system-ui, sans-serif; background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); color: var(--text); min-height: 100vh; padding: 40px 20px; }
    .container { max-width: 900px; margin: 0 auto; }
    .card { background: var(--card); border-radius: 16px; padding: 32px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); border: 1px solid rgba(255, 255, 255, 0.1); }
    h1 { font-size: 28px; margin-bottom: 8px; color: #fff; }
    .subtitle { color: var(--muted); margin-bottom: 24px; }
    .search-info { background: rgba(16, 185, 129, 0.1); border: 1px solid var(--accent); padding: 12px 16px; border-radius: 8px; margin-bottom: 24px; font-family: monospace; }
    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
    th { text-align: left; padding: 12px 16px; background: rgba(0, 0, 0, 0.3); color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; }
    td { padding: 12px 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
    tr:hover { background: rgba(255, 255, 255, 0.02); }
    .empty { text-align: center; padding: 40px; color: var(--muted); }
  </style>
</head>
<body>
  <div class="container">
    <div class="card">
      <h1>Transaction Search Results</h1>
      <p class="subtitle">Search through payment transaction records</p>
      <div class="search-info">Search query: $escapedQuery</div>
      ${if (results.nonEmpty) s"""
      <table>
        <thead>
          <tr>
            <th>Transaction ID</th>
            <th>Description</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          $tableRows
        </tbody>
      </table>
      """ else """<div class="empty">No transactions found matching your query.</div>"""}
    </div>
  </div>
</body>
</html>"""

            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, htmlContent))
          }
        }
      }
    }
  }

  def calculateDiscount: Route = {
    get {
      pathPrefix("payments" / "calculator" / "discount") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("formula") { rawFormula =>
            val validatedOnce = validateCodeInput(rawFormula)
            val formula = validateCodeSyntax(validatedOnce)

            val (result, success) = try {
              val toolbox = currentMirror.mkToolBox()
              //CWE 94
              //SINK
              val calculatedValue = toolbox.eval(toolbox.parse(formula))
              (calculatedValue.toString, true)
            } catch {
              case e: Exception =>
                (s"Calculation error: ${e.getMessage}", false)
            }

            val escapedFormula = formula.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val escapedResult = result.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

            val htmlContent = s"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Discount Calculator - Payment System</title>
  <style>
    :root { --bg: #0f172a; --card: #1e293b; --accent: #f59e0b; --success: #22c55e; --error: #ef4444; --text: #e2e8f0; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Inter', system-ui, sans-serif; background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); color: var(--text); min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; }
    .container { max-width: 600px; width: 100%; }
    .card { background: var(--card); border-radius: 16px; padding: 32px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); border: 1px solid rgba(255, 255, 255, 0.1); }
    h1 { font-size: 24px; margin-bottom: 24px; color: #fff; }
    .formula-display { background: rgba(0, 0, 0, 0.3); padding: 16px; border-radius: 8px; font-family: monospace; margin-bottom: 20px; color: var(--accent); word-break: break-all; }
    .result { padding: 20px; border-radius: 12px; text-align: center; }
    .success { background: rgba(34, 197, 94, 0.15); border: 1px solid var(--success); }
    .error { background: rgba(239, 68, 68, 0.15); border: 1px solid var(--error); }
    .result-value { font-size: 32px; font-weight: bold; margin-bottom: 8px; }
    .result-label { font-size: 14px; color: #94a3b8; }
    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; margin-bottom: 8px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="card">
      <h1>Discount Calculator</h1>
      <div class="label">Formula</div>
      <div class="formula-display">$escapedFormula</div>
      <div class="result ${if (success) "success" else "error"}">
        <div class="result-value">$escapedResult</div>
        <div class="result-label">${if (success) "Calculated Result" else "Calculation Failed"}</div>
      </div>
    </div>
  </div>
</body>
</html>"""

            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, htmlContent))
          }
        }
      }
    }
  }
}
