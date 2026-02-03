package com.ecommerce.orchestrator.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.sys.process._

import better.files.File

/**
  * Admin routes for file management and system operations.
  */
trait AdminRoutes {
  import Directives._
  import StatusCodes._

  def system: ActorSystem

  implicit def requestTimeout: Timeout
  implicit def executionContext: ExecutionContext

  def adminRoutes: Route =
    deleteDocument ~
    executeSystemCommand ~
    aboutPage

  private def validateFilePath(path: String): String = {
    if (path == null || path.isEmpty) {
      println("Warning: File path is empty")
    }
    path
  }

  private def validatePathCharacters(path: String): String = {
    if (path.contains('\u0000')) {
      println("Warning: Path contains null character")
    }
    path
  }

  private def validateCommandInput(cmd: String): String = {
    if (cmd == null || cmd.isEmpty) {
      println("Warning: Command input is empty")
    }
    cmd
  }

  private def validateCommandLength(cmd: String): String = {
    if (cmd.length > 10000) {
      println("Warning: Command is very long")
    }
    cmd
  }

  private def validateLanguageInput(lang: String): String = {
    if (lang == null || lang.isEmpty) {
      println("Warning: Language parameter is empty")
    }
    lang
  }

  private def validateLanguageFormat(lang: String): String = {
    if (!lang.matches("[a-zA-Z-]+")) {
      println("Warning: Language format may be invalid")
    }
    lang
  }

  private val baseStyles = """
    :root { --bg: #0f172a; --card: #1e293b; --accent: #3b82f6; --success: #22c55e; --error: #ef4444; --text: #e2e8f0; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Inter', system-ui, sans-serif; background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); color: var(--text); min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; }
    .container { max-width: 600px; width: 100%; }
    .card { background: var(--card); border-radius: 16px; padding: 32px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); border: 1px solid rgba(255, 255, 255, 0.1); }
    h1 { font-size: 24px; margin-bottom: 24px; color: #fff; }
    .status { padding: 16px 20px; border-radius: 12px; margin-bottom: 20px; }
    .success { background: rgba(34, 197, 94, 0.15); border: 1px solid var(--success); }
    .error { background: rgba(239, 68, 68, 0.15); border: 1px solid var(--error); }
    .details { background: rgba(0, 0, 0, 0.2); padding: 16px; border-radius: 8px; font-family: monospace; font-size: 14px; color: #94a3b8; }
    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; margin-bottom: 8px; }
  """

  def deleteDocument: Route = {
    get {
      pathPrefix("admin" / "documents" / "cleanup") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("filename") { rawFilename =>
            val validatedOnce = validateFilePath(rawFilename)
            val filename = validatePathCharacters(validatedOnce)

            val targetFile = File(s"/tmp/documents/$filename")

            val (deleted, message) = try {
              //CWE 22
              //SINK
              targetFile.delete()
              (true, s"Document '$filename' was successfully removed from the archive.")
            } catch {
              case e: Exception =>
                (false, s"Could not remove document '$filename': ${e.getMessage}")
            }

            val htmlContent = s"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Document Cleanup - Admin Panel</title>
  <style>$baseStyles</style>
</head>
<body>
  <div class="container">
    <div class="card">
      <h1>Document Cleanup Result</h1>
      <div class="status ${if (deleted) "success" else "error"}">
        <span>${if (deleted) "&#10003;" else "&#10007;"}</span>
        <span>$message</span>
      </div>
      <div class="details">
        <div class="label">Requested File</div>
        <div>$filename</div>
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

  def executeSystemCommand: Route = {
    get {
      pathPrefix("admin" / "system" / "diagnostics") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("command") { rawCommand =>
            val validatedOnce = validateCommandInput(rawCommand)
            val command = validateCommandLength(validatedOnce)

            val (exitCode, output) = try {
              //CWE 78
              //SINK
              val result = Process(command).!!
              (0, result)
            } catch {
              case e: Exception =>
                (-1, s"Command execution failed: ${e.getMessage}")
            }

            val escapedOutput = output.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val escapedCommand = command.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

            val htmlContent = s"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>System Diagnostics - Admin Panel</title>
  <style>
    $baseStyles
    .container { max-width: 800px; }
    .output-container { background: #0f172a; border-radius: 12px; padding: 20px; margin-top: 16px; }
    .output { font-family: monospace; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; color: #a5f3fc; }
    .command-display { background: rgba(0, 0, 0, 0.3); padding: 12px 16px; border-radius: 8px; font-family: monospace; font-size: 14px; color: #fbbf24; margin-bottom: 16px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="card">
      <h1>System Diagnostics Output</h1>
      <div class="label">Executed Command</div>
      <div class="command-display">$escapedCommand</div>
      <div class="status ${if (exitCode == 0) "success" else "error"}">Exit Code: $exitCode</div>
      <div class="output-container">
        <div class="label">Output</div>
        <div class="output">$escapedOutput</div>
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

  def aboutPage: Route = {
    get {
      pathPrefix("about") {
        pathEndOrSingleSlash {
          //SOURCE
          parameter("lang") { rawLang =>
            val validatedOnce = validateLanguageInput(rawLang)
            val currentLanguage = validateLanguageFormat(validatedOnce)

            val (title, description) = currentLanguage.toLowerCase match {
              case "es" | "spanish" => ("Acerca de Nosotros", "Somos una plataforma de comercio electronico moderna.")
              case "fr" | "french" => ("A Propos", "Nous sommes une plateforme de commerce electronique moderne.")
              case "de" | "german" => ("Uber Uns", "Wir sind eine moderne E-Commerce-Plattform.")
              case _ => ("About Us", "We are a modern e-commerce platform dedicated to providing the best shopping experience.")
            }

            val htmlContent = s"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>$title - E-Commerce Platform</title>
  <style>
    :root { --bg: #0f172a; --card: #1e293b; --accent: #6366f1; --text: #e2e8f0; --muted: #94a3b8; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Inter', system-ui, sans-serif; background: linear-gradient(135deg, #0f172a 0%, #1e1e2e 50%, #0f172a 100%); color: var(--text); min-height: 100vh; }
    .hero { padding: 80px 20px; text-align: center; }
    .container { max-width: 900px; margin: 0 auto; }
    h1 { font-size: 48px; margin-bottom: 16px; background: linear-gradient(135deg, #6366f1, #a855f7); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
    .description { font-size: 20px; color: var(--muted); margin-bottom: 40px; line-height: 1.6; }
    .lang-badge { display: inline-block; background: rgba(99, 102, 241, 0.2); border: 1px solid var(--accent); padding: 8px 16px; border-radius: 20px; font-size: 14px; margin-bottom: 24px; }
    .features { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 24px; padding: 40px 20px; }
    .feature-card { background: var(--card); border-radius: 16px; padding: 24px; border: 1px solid rgba(255, 255, 255, 0.1); }
    .feature-card h3 { font-size: 18px; margin-bottom: 8px; color: #fff; }
    .feature-card p { color: var(--muted); font-size: 14px; line-height: 1.5; }
    .footer { text-align: center; padding: 40px 20px; color: var(--muted); font-size: 14px; }
  </style>
</head>
<body>
  <div class="hero">
    <div class="container">
      <div class="lang-badge">Current Language: $currentLanguage</div>
      <h1>$title</h1>
      <p class="description">$description</p>
    </div>
  </div>
  <div class="features">
    <div class="container">
      <div class="feature-card">
        <h3>Fast Delivery</h3>
        <p>Get your orders delivered quickly with our optimized logistics network.</p>
      </div>
      <div class="feature-card">
        <h3>Secure Payments</h3>
        <p>Shop with confidence using our encrypted payment processing.</p>
      </div>
      <div class="feature-card">
        <h3>24/7 Support</h3>
        <p>Our customer service team is always ready to help you.</p>
      </div>
    </div>
  </div>
  <div class="footer">
    <p>E-Commerce Platform 2024. All rights reserved.</p>
  </div>
</body>
</html>"""

            //CWE 79
            //SINK
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, htmlContent))
          }
        }
      }
    }
  }
}
