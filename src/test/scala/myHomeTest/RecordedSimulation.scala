package myHomeTest

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class RecordedSimulation extends Simulation {

	val httpProtocol = http
		.baseUrl("https://demo.nopcommerce.com")
		//.inferHtmlResources()
		.acceptHeader("image/webp,*/*")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
		.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:82.0) Gecko/20100101 Firefox/82.0")
		.disableFollowRedirect
		.check(status.is(200))

	val headers_1 = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
		"Upgrade-Insecure-Requests" -> "1")

	val headers_2 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"X-Requested-With" -> "XMLHttpRequest")

	val headers_3 = Map(
		"Accept" -> "*/*",
		"Content-Type" -> "application/x-www-form-urlencoded; charset=UTF-8",
		"Origin" -> "https://demo.nopcommerce.com",
		"X-Requested-With" -> "XMLHttpRequest")

	val feeder = csv("search.csv").random

	val scn = scenario("RecordedSimulation")
		.exec(http("OpenSite")
			.get("/")
			.headers(headers_1)
			.check(regex("<title>(.+?)</title>").is("nopCommerce demo store")))
		.pause(1, 5)
		.feed(feeder)
		.exec(http("SearchProduct")
			.get("/catalog/searchtermautocomplete?term=${searchRequest}")
			.headers(headers_2))
		.pause(1, 5)
		.exec(http("OpenSearchPage")
			.get("/search?q=${searchRequest}")
			.headers(headers_1)
			.check(css("a:contains('${searchRequest}')", "href").findRandom.saveAs("PDPUrl")))
		.pause(1, 5)
		.exec(http("OpenPDP")
			.get("${PDPUrl}")
			.check(css("input[class='button-1 add-to-cart-button']", "data-productid").saveAs("productId"))
			.headers(headers_1))
		.pause(1, 5)
		.exec(http("AddToCart")
			.post(s"/addproducttocart/details/" + "${productId}" + "/1")
			.headers(headers_3)
			.formParam("addtocart_${productId}.EnteredQuantity", "1")
			.formParam("CountryId", "0")
			.formParam("StateProvinceId", "0")
			.formParam("ZipPostalCode", "")
			.formParam("__RequestVerificationToken", "CfDJ8NJzpPdWJDZGtf_4GVVpZ2nl3qxoxLvjTSiRtas-gQp63k1Bo7P_BVqFqyunru1BhB2M02CWoIoH4MfHK76XkxiqZ6rNtam7QxFOhosiUIFeOYHq8qs1t5t-CH0z31zpn5vKEMyC3-IGbUGRbU3MR6w")
			.check(substring("success\":true,\"message\":\"The product has been added to your <a href=\\\"/cart\\\">shopping cart</a>")))
		.pause(1, 5)
		.exec(http("GoToCart")
			.get("/cart")
			.headers(headers_1)
			.check(substring("${PDPUrl}")))
		.pause(1)

	setUp(scn.inject(rampUsers(5) during (15 seconds))).protocols(httpProtocol).assertions(
		global.responseTime.max.lt(1200),
		global.successfulRequests.percent.gt(95)
	)
}