/*
 * Copyright 2012 @peltchu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scatter

import org.scalatest.FunSuite
import org.scalatest.matchers.MustMatchers
import cc.spray.http.HttpMethods._
import cc.spray.http.HttpHeaders._
import cc.spray.http.HttpCharsets._
import cc.spray.http.CacheDirectives._
import cc.spray.http.MediaTypes._
import cc.spray.http._

class TransformationsSuite extends FunSuite with MustMatchers with
                                        Transformations {
	
	val req = HttpRequest()

	test("request methods") {
		
		val g = GET("foo").transform(HttpRequest(HttpMethods.POST))
		g.method must be (HttpMethods.GET)
		g.uri must be ("/foo")

		val p = POST("foo").transform(req)
		p.method must be (HttpMethods.POST)
		p.uri must be ("/foo")
		
		val pt = PUT("foo").transform(req)
		pt.method must be (HttpMethods.PUT)
		pt.uri must be ("/foo")
		
		val d = DELETE("foo").transform(req)
		d.method must be (HttpMethods.DELETE)
		d.uri must be ("/foo")
		
		val h = HEAD("foo").transform(req)
		h.method must be (HttpMethods.HEAD)
		h.uri must be ("/foo")

		val opt = OPTIONS("foo").transform(req)
		opt.method must be (HttpMethods.OPTIONS)
		opt.uri must be ("/foo")
		
		val t = TRACE("foo").transform(req)
		t.method must be (HttpMethods.TRACE)
		t.uri must be ("/foo")

	}
	
	test("path") {
		val p1 = path("foobar").transform(req)
		val p2 = path("/baz").transform(p1)

		p1.uri must be ("/foobar")
		p2.uri must be ("/foobar/baz")
	}

	test("header") {
		val h = header("Pragma", "no-cache").transform(req)
		val h2 = header(`Cache-Control`(`no-store`)).transform(req)

		h.headers.exists(h => h.name == "Pragma" && h.value == "no-cache")	must be (true)
		h2.headers.exists (h => h.name == "Cache-Control" && h.value == "no-store") must be (true)
	}

	test("charset") {
		charset(`UTF-8`).transform(req).content.get.contentType.charset.get must be (`UTF-8`)
	}

	test("charset (content already existing)") {
		val existingContentReq = HttpRequest(HttpMethods.GET, "", List(), Some(HttpContent("foobar")))

		intercept[UnsupportedOperationException] {
			charset(`UTF-8`).transform(existingContentReq)
		}
	}
	
	test("json") {
		import cc.spray.typeconversion.SprayJsonSupport._
		import cc.spray.json.DefaultJsonProtocol._

		val c = json(Map("foo" -> "bar")).transform(req)

		c.content.as[Map[String,String]].right.get must be(Map("foo" -> "bar"))
	}
	
	test("charset + json") {
		import cc.spray.typeconversion.SprayJsonSupport._
		import cc.spray.json.DefaultJsonProtocol._

		val definedCharsetReq = HttpRequest(HttpMethods.GET, "", List(), Some(HttpContent(ContentType(`text/plain`, `UTF-8`), "")))
		val c = json(Map("foo" -> "bar")).transform(definedCharsetReq)

		c.content.as[Map[String,String]].right.get must be(Map("foo" -> "bar"))
		c.content.get.contentType.charset.get must be (`UTF-8`)
	}
	
	test("form") {
		import cc.spray.typeconversion._
		import cc.spray.typeconversion.DefaultMarshallers._
		import cc.spray.typeconversion.DefaultUnmarshallers._

		val c = form("foo" -> "bar").transform(req)

		c.content.as[FormData].right.get must be (FormData(Map("foo" -> "bar")))
	}
	
	test("charset + form") {
		import cc.spray.typeconversion._
		import cc.spray.typeconversion.DefaultMarshallers._
		import cc.spray.typeconversion.DefaultUnmarshallers._

		val definedCharsetReq = HttpRequest(HttpMethods.GET, "", List(), Some(HttpContent(ContentType(`text/plain`, `UTF-8`), "")))

		val c = form("foo" -> "bar").transform(definedCharsetReq)

		c.content.as[FormData].right.get must be (FormData(Map("foo" -> "bar")))
		c.content.get.contentType.charset.get must be (`UTF-8`)
	}

}
